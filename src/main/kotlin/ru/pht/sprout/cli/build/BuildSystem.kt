package ru.pht.sprout.cli.build

import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.lexer.Lexer
import ru.pht.sprout.module.header.parser.Parser
import ru.pht.sprout.module.header.parser.ParserException
import ru.pht.sprout.module.repo.ICachingRepository
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.impl.GiteaRepository
import ru.pht.sprout.module.repo.impl.GithubRepository
import ru.pht.sprout.module.repo.impl.LocalCacheRepository
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.notExists
import kotlin.io.path.readText

open class BuildSystem {
    // ===== ДИРЕКТОРИИ ===== //
    var sproutDirectory = Path("${System.getProperty("user.home")}/.sprout/").absolutePathString()
    var moduleDirectory = Path(".").absolutePathString()
    // ===== РЕПОЗИТОРИИ ===== //
    val repositories: MutableList<IRepository> = mutableListOf(GithubRepository(), GiteaRepository())
    val cachingRepository: ICachingRepository = LocalCacheRepository(this)
    // ===== МОДУЛЬ ===== //
    var moduleHeader: ModuleHeader? = null
    var moduleHeaderError: ParserException? = null

    // ===== API ===== //

    fun tryParseModule(): Boolean {
        val headerFile = Path(this.moduleDirectory).resolve("module.pht")
        if (headerFile.notExists())
            return false
        try {
            this.moduleHeader = Parser(Lexer(headerFile.readText(Charsets.UTF_8))).parse()
            return true
        } catch (e: ParserException) {
            this.moduleHeaderError = e
            return false
        }
    }
}