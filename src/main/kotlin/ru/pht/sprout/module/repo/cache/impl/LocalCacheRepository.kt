package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import ru.pht.sprout.cli.build.BuildInfo
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import ru.pht.sprout.module.utils.ZipUtils
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

class LocalCacheRepository(
    private val cacheDirectory: Path,
    private val cacheListFile: Path,
    private val repositories: List<IRepository>
) : ICachingRepository {
    private val localized: MutableList<MaybeCachedDownloadable>
    private val cached: MutableSet<IDownloadable>

    constructor(buildSystem: BuildInfo) : this(
        File("${buildSystem.sproutDirectory}/cache/modules").toPath(),
        File("${buildSystem.sproutDirectory}/cache/modules.json").toPath(),
        buildSystem.repositories
    )

    init {
        if (this.cacheDirectory.notExists()) {
            this.cacheDirectory.createDirectories()
            this.cacheListFile.deleteIfExists()
            this.localized = ArrayList()
            this.cached = HashSet()
        } else {
            if (this.cacheListFile.exists()) {
                this.localized = Json.Default.decodeFromString(this.cacheListFile.readText(Charsets.UTF_8))
                this.cached = this.localized.toMutableSet()
            } else {
                this.localized = ArrayList()
                this.cached = HashSet()
            }
        }
    }

    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> {
        // Ищем ссылки на скачивание
        val find: MutableMap<Version, MutableList<IDownloadable>> = HashMap()
        this.repositories.forEach { repository ->
            if (repository is ICachingRepository)
                return@forEach
            repository.findAllAsync()
                .stream()
                .filter { it.name == name && version.isSatisfiedBy(it.version) }
                .forEach {
                    val list = find[it.version]
                    if (list == null)
                        find[it.version] = arrayListOf(it)
                    else list += it
                }
        }
        // Верифицируем и сортируем
        val verified: SortedSet<IDownloadable> = TreeSet()
        find.values.forEach { links ->
            // Собираем хеши
            val hashes: MutableMap<String, MutableList<IDownloadable>> = HashMap()
            links.forEach {
                val list = hashes[it.hash]
                if (list == null)
                    hashes[it.hash] = arrayListOf(it)
                else list += it
            }
            // Выбираем ссылки где больше хешей совпадают
            val values = hashes.values.iterator()
            var many: List<IDownloadable> = values.next()
            for (other in values) {
                if (many.size < other.size) {
                    many = other
                }
            }
            // Преобразуем, добавляем к списку верифицированных и кешу
            many.stream()
                .map { MaybeCachedDownloadable(this, it, this.cacheDirectory.resolve(it.name + "." + it.version + ".zip").normalize().absolutePathString()) }
                .forEach {
                    verified += it
                    this.cached += it
                }
        }
        // Возвращаем
        return verified.toList()
    }

    override suspend fun findAllAsync(): List<IDownloadable> {
        // Ищем ссылки на скачивание и распределяем
        val find: MutableMap<String, MutableMap<Version, MutableMap<String, MutableList<IDownloadable>>>> = HashMap()
        this.repositories.forEach { repository ->
            if (repository is ICachingRepository)
                return@forEach
            repository.findAllAsync().forEach { download ->
                var versions = find[download.name]
                if (versions == null) {
                    versions = HashMap()
                    find[download.name] = versions
                }
                var hashes = versions[download.version]
                if (hashes == null) {
                    hashes = HashMap()
                    versions[download.version] = hashes
                }
                var downloads = hashes[download.hash]
                if (downloads == null) {
                    downloads = ArrayList()
                    hashes[download.hash] = downloads
                }
                downloads += download
            }
        }
        // Верифицируем и добавляем в хеш
        val verified = this.cached
        find.values.forEach { modules ->
            modules.values.forEach { versions ->
                // Выбираем ссылки где больше хешей совпадают
                val values = versions.values.iterator()
                var many: List<IDownloadable> = values.next()
                for (other in values) {
                    if (many.size < other.size) {
                        many = other
                    }
                }
                // Преобразуем, добавляем к списку верифицированных и кешу
                for (it in many) {
                    verified += MaybeCachedDownloadable(this, it, this.cacheDirectory.resolve(it.name + "." + it.version + ".zip").normalize().absolutePathString())
                }
            }
        }
        // Возвращаем
        return verified.toList()
    }

    override fun findAllCached(): List<IDownloadable> =
        this.localized

    override suspend fun findAllCachedAsync(): List<IDownloadable> =
        this.localized

    private fun saveLocalizedList() {
        if (this.cacheListFile.parent.notExists())
            this.cacheListFile.parent.createDirectories()
        this.cacheListFile.writeText(Json.Default.encodeToString(this.localized))
    }

    @Serializable
    private class MaybeCachedDownloadable : IDownloadable, Comparable<MaybeCachedDownloadable> {
        override val name: String
        override val version: Version
        override val hash: String
        val file: String
        @Transient
        var repository: LocalCacheRepository? = null
        @Transient
        var original: IDownloadable? = null

        constructor(repository: LocalCacheRepository, original: IDownloadable, file: String) {
            this.repository = repository
            this.original = original
            this.name = original.name
            this.version = original.version
            this.hash = original.hash
            this.file = file
        }

        override suspend fun headerAsync(): ModuleHeader {
            this.checkDownloadCache()
            return withContext(Dispatchers.IO) {
                ZipUtils.unzipHeader(FileInputStream(this@MaybeCachedDownloadable.file))
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
                this.original!!.downloadZipAsync(File(this.file).toPath())
                this.repository!!.localized += this
                this.repository!!.saveLocalizedList()
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

        override fun compareTo(other: MaybeCachedDownloadable): Int =
            this.version.compareTo(other.version)
    }
}