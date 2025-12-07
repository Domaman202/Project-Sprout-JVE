package ru.pht.sprout.module.repo.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import ru.pht.sprout.build.BuildSystem
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.repo.ICachingRepository
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.utils.ZipUtils
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class LocalCacheRepository(
    private val cacheDirectory: Path,
    private val cacheListFile: Path,
    private val repositories: List<IRepository>
) : ICachingRepository {
    private val localized: MutableList<MaybeCachedDownloadable>
    private val cached: MutableList<IDownloadable>

    constructor(buildSystem: BuildSystem) : this(
        File("${buildSystem.sproutDirectory}/cache/modules").toPath(),
        File("${buildSystem.sproutDirectory}/cache/modules.json").toPath(),
        buildSystem.repositories
    )

    init {
        if (this.cacheDirectory.notExists()) {
            this.cacheDirectory.createDirectories()
            this.cacheListFile.deleteIfExists()
            this.localized = ArrayList()
            this.cached = ArrayList()
        } else {
            if (this.cacheListFile.exists()) {
                this.localized = Json.decodeFromString(this.cacheListFile.readText(Charsets.UTF_8))
                this.cached = this.localized.toMutableList()
            } else {
                this.localized = ArrayList()
                this.cached = ArrayList()
            }
        }
    }

    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> =
        this
            .findAllAsync()
            .asSequence()
            .filter { it.name == name && version.isSatisfiedBy(it.version) }
            .toMutableList()
            .sortedBy { it.version }

    override suspend fun findAllAsync(): List<IDownloadable> {
        val list = this.cached
        this.repositories.asSequence().filter { it !is ICachingRepository }.forEach { repo ->
            repo.findAllAsync().forEach { other ->
                if (list.none { it.name == other.name && it.version == other.version }) {
                    list += MaybeCachedDownloadable(this, other, this.cacheDirectory.resolve(other.name).normalize().absolutePathString())
                }
            }
        }
        return list
    }

    override fun findAllCached(): List<IDownloadable> =
        this.localized

    override suspend fun findAllCachedAsync(): List<IDownloadable> =
        this.localized

    private fun saveLocalizedList() {
        if (this.cacheListFile.parent.notExists())
            this.cacheListFile.parent.createDirectories()
        this.cacheListFile.writeText(Json.encodeToString(this.localized))
    }

    @Serializable
    private class MaybeCachedDownloadable : IDownloadable {
        override val name: String
        override val version: Version
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
            this.file = file
        }

        override suspend fun headerAsync(): ModuleHeader {
            this.checkDownloadCache()
            return ZipUtils.unzipHeader(FileInputStream(this.file))
        }

        override suspend fun downloadAsync(dir: Path) {
            this.checkDownloadCache()
            val dir = dir.resolve(this.name).normalize()
            if (dir.notExists())
                dir.createDirectories()
            ZipUtils.unzip(dir, Files.readAllBytes(File(this.file).toPath()))
        }

        override suspend fun downloadZipAsync(file: Path) {
            if (file.parent.notExists())
                file.parent.createDirectories()
            this.checkDownloadCache()
            Files.write(file, Files.readAllBytes(File(this.file).toPath()))
        }

        private suspend fun checkDownloadCache() {
            if (Files.notExists(File(this.file).toPath())) {
                this.original!!.downloadZipAsync(File(this.file).toPath())
                this.repository!!.localized += this
                this.repository!!.saveLocalizedList()
            }
        }
    }
}