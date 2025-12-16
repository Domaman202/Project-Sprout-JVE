package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.constraints.Constraint
import ru.pht.sprout.cli.build.BuildInfo
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import ru.pht.sprout.module.utils.RepoUtils

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
}