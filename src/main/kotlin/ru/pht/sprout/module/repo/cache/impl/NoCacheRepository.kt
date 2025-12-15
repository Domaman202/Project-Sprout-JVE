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
        RepoUtils.findAndVerifySortedAsync(this.repositories, name, version, CombinedDownloadable::combineAndAdd)

    override suspend fun findAllAsync(): List<IDownloadable> {
        val verified: MutableList<IDownloadable> = ArrayList()
        RepoUtils.findAllAndVerifyAsync(repositories) { verified += CombinedDownloadable.of(it) }
        return verified
    }

    override suspend fun findAllCachedAsync(): List<IDownloadable> =
        emptyList()

    class CombinedDownloadable(
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


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CombinedDownloadable
            return this.hash == other.hash && this.originals == other.originals
        }

        override fun hashCode(): Int =
            this.hash.hashCode() + this.originals.hashCode() * 31

        private inline fun <T> tryAllSources(block: IDownloadable.() -> T): T {
            for (downloadable in this.originals) {
                try {
                    return block(downloadable)
                } catch (_: IOException) {
                }
            }
            throw IOException("Все источники '${this.name}' недоступны")
        }

        companion object {
            @Suppress("NOTHING_TO_INLINE")
            inline fun of(combine: List<IDownloadable>): CombinedDownloadable {
                val first = combine.first()
                return CombinedDownloadable(combine, first.name, first.version, first.hash)
            }

            inline fun combineAndAdd(combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) {
                addTo(of(combine))
            }
        }
    }
}