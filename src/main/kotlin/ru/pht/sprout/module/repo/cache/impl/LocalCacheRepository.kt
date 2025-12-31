package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import ru.pht.sprout.module.utils.RepoUtils
import ru.pht.sprout.module.utils.ZipUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Локальный кеширующий репозиторий.
 *
 * @param cacheDirectory Папка хранения кешированных модулей.
 * @param cacheListFile Файл списка кешированных модулей.
 * @param lastInvalidationTimeFile Файл времени последнего обновления кеша.
 * @param repositories Репозитории загрузки.
 * @param invalidationPeriod Период между обновлениями кеша, `-1` - никогда не обновлять кеш без необходимости, `0` - всегда обновлять кеш.
 */
class LocalCacheRepository(
    private val cacheDirectory: Path,
    private val cacheListFile: Path,
    private val lastInvalidationTimeFile: Path,
    private val repositories: List<IRepository>,
    private val invalidationPeriod: Duration
) : ICachingRepository {
    private var lastInvalidationTime: Duration
    private val localized: MutableList<MaybeCachedDownloadable>
    private val cached: MutableSet<IDownloadable>

    constructor(workdir: Path, repositories: List<IRepository>, invalidationPeriod: Duration) : this(
        workdir.resolve("cache/modules"),
        workdir.resolve("cache/modules.json"),
        workdir.resolve("cache/modules.lastInvalidationTime.raw"),
        repositories,
        invalidationPeriod,
    )

    init {
        if (this.cacheDirectory.notExists()) {
            this.cacheDirectory.createDirectories()
            this.cacheListFile.deleteIfExists()
            this.lastInvalidationTimeFile.deleteIfExists()
            this.lastInvalidationTime = Duration.ZERO
            this.localized = ArrayList()
            this.cached = HashSet()
        } else {
            this.lastInvalidationTime =
                if (this.lastInvalidationTimeFile.exists())
                    Duration.parse(this.lastInvalidationTimeFile.readText(Charsets.UTF_8))
                else Duration.ZERO
            this.localized =
                if (this.cacheListFile.exists())
                    Json.decodeFromString(this.cacheListFile.readText(Charsets.UTF_8))
                else ArrayList()
            this.cached = this.localized.toMutableSet()
        }
    }

    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> =
        checkCacheValidation(
            this.cached.filterTo(ArrayList()) { it.name == name && version.isSatisfiedBy(it.version) },
            { it },
            { RepoUtils.findUnavailableAndVerifySortedAsync(this.repositories, it, name, version, this::tryAddCache) }
        )

    override suspend fun findAllAsync(): List<IDownloadable> =
        checkCacheValidation(
            this.cached,
            { it.toList() },
            { RepoUtils.findAllAndVerifyAsync(this.repositories) { tryAddCache(it) { } } }
        )

    override fun findAllCached(): List<IDownloadable> =
        this.localized

    override suspend fun findAllCachedAsync(): List<IDownloadable> =
        this.findAllCached()

    /**
     * @return [invalidationPeriod].
     */
    internal fun getInvalidationPeriod(): Duration =
        this.invalidationPeriod

    private inline fun <T : Collection<IDownloadable>> checkCacheValidation(cached: T, map: (T) -> List<IDownloadable>, update: (T) -> Unit): List<IDownloadable> {
        if (cached.isNotEmpty()) {
            if (this.invalidationPeriod.isNegative())
                return map(cached)
            if (this.invalidationPeriod.isPositive()) {
                val time = System.currentTimeMillis().milliseconds
                if (time - this.lastInvalidationTime < this.invalidationPeriod)
                    return map(cached)
                this.lastInvalidationTime = time
                this.lastInvalidationTimeFile.writeText(time.toString(), Charsets.UTF_8)
            }
        }
        update(cached)
        return map(cached)
    }

    private inline fun tryAddCache(combine: (List<IDownloadable>), addTo: (IDownloadable) -> Unit) {
        val first = combine.first()
        val find = this.cached.find { it.hash == first.hash }
        if (find != null) {
            addTo(find)
            return
        }
        val cache = MaybeCachedDownloadable(this, combine, first)
        this.cached += cache
        addTo(cache)
    }

    private fun saveLocalizedList() {
        if (this.cacheListFile.parent.notExists())
            this.cacheListFile.parent.createDirectories()
        this.cacheListFile.writeText(Json.encodeToString(this.localized))
    }

    @Serializable
    private class MaybeCachedDownloadable : IDownloadable {
        override val name: String
        override val version: Version
        override val hash: String
        val file: String
        @Transient
        var repository: LocalCacheRepository? = null
        @Transient
        var originals: List<IDownloadable>? = null

        constructor(repository: LocalCacheRepository, originals: List<IDownloadable>, first: IDownloadable) {
            this.repository = repository
            this.originals = originals
            this.file = repository.cacheDirectory.resolve(first.name + "." + first.version + ".zip").normalize().absolutePathString()
            this.name = first.name
            this.version = first.version
            this.hash = first.hash
        }

        override suspend fun headerAsync(): ModuleHeader {
            this.checkDownloadCache()
            return withContext(Dispatchers.IO) {
                ZipUtils.unzipHeader(Files.readAllBytes(File(this@MaybeCachedDownloadable.file).toPath()))
            }
        }

        override suspend fun downloadAsync(dir: Path) {
            this.checkDownloadCache()
            val dir = dir.resolve(this.name).normalize()
            if (dir.notExists())
                dir.createDirectories()
            ZipUtils.unzip(
                dir,
                withContext(Dispatchers.IO) {
                    Files.readAllBytes(File(this@MaybeCachedDownloadable.file).toPath())
                }
            )
        }

        override suspend fun downloadZipAsync(file: Path) {
            if (file.parent.notExists())
                file.parent.createDirectories()
            this.checkDownloadCache()
            withContext(Dispatchers.IO) {
                Files.write(file, Files.readAllBytes(File(this@MaybeCachedDownloadable.file).toPath()))
            }
        }

        private suspend fun checkDownloadCache() {
            if (Files.notExists(File(this.file).toPath())) {
                for (downloadable in this.originals!!) {
                    try {
                        downloadable.downloadZipAsync(File(this.file).toPath())
                        this.repository!!.localized += this
                        this.repository!!.saveLocalizedList()
                        return
                    } catch (_: IOException) {
                    }
                }
                throw IOException("Все источники '${this.name}' недоступны")
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as MaybeCachedDownloadable
            return this.file == other.file
        }

        override fun hashCode(): Int =
            this.file.hashCode()
    }
}