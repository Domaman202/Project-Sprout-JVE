package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import kotlinx.io.IOException
import ru.pht.sprout.cli.build.BuildInfo
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import ru.pht.sprout.module.utils.RepoUtils
import java.nio.file.Path

class NoCacheRepository(private val repositories: List<IRepository>) : ICachingRepository {
    constructor(buildSystem: BuildInfo) : this(buildSystem.repositories)

    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> =
        RepoUtils.findAndVerifySortedAsync(this.repositories, name, version, this::combineAndAdd)

    override suspend fun findAllAsync(): List<IDownloadable> {
        val verified: MutableList<IDownloadable> = ArrayList()
        RepoUtils.findAllAndVerifyAsync(repositories) { verified += combine(it) }
        return verified
    }

    override fun findAllCached(): List<IDownloadable> =
        emptyList()

    override suspend fun findAllCachedAsync(): List<IDownloadable> =
        emptyList()

    private inline fun combineAndAdd(combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) {
        addTo(this.combine(combine))
    }

    private fun combine(combine: List<IDownloadable>): IDownloadable {
        val first = combine.first()
        return CombinedDownloadable(combine, first.name, first.version, first.hash)
    }

    private class CombinedDownloadable(
        val originals: List<IDownloadable>,
        override val name: String,
        override val version: Version,
        override val hash: String
    ) : IDownloadable {
        override fun header(): ModuleHeader = tryAllSources {
            header()
        }

        override suspend fun headerAsync(): ModuleHeader = tryAllSources {
            headerAsync()
        }

        override fun download(dir: Path) = tryAllSources {
            download(dir)
        }

        override suspend fun downloadAsync(dir: Path) = tryAllSources {
            downloadAsync(dir)
        }

        override fun downloadZip(file: Path) = tryAllSources {
            downloadZip(file)
        }

        override suspend fun downloadZipAsync(file: Path) = tryAllSources {
            downloadZipAsync(file)
        }

        private inline fun <T> tryAllSources(block: IDownloadable.() -> T): T {
            for (downloadable in this.originals) {
                try {
                    return block(downloadable)
                } catch (_: IOException) {
                }
            }
            throw IOException("Все источники '${this.name}' недоступны")
        }
    }
}