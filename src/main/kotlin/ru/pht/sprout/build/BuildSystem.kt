package ru.pht.sprout.build

import ru.pht.sprout.module.repo.ICachingRepository
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.impl.GiteaRepository
import ru.pht.sprout.module.repo.impl.GithubRepository
import ru.pht.sprout.module.repo.impl.LocalCacheRepository
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

open class BuildSystem {
    // ===== ДИРЕКТОРИИ ===== //
    var sproutDirectory = Path("${System.getProperty("user.home")}/.sprout/").absolutePathString()
    var moduleDirectory = Path(".").absolutePathString()
    // ===== РЕПОЗИТОРИИ ===== //
    val repositories: MutableList<IRepository> = mutableListOf(GithubRepository(), GiteaRepository())
    val cachingRepository: ICachingRepository = LocalCacheRepository(this)
}