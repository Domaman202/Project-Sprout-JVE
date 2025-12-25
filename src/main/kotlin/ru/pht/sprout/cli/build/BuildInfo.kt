package ru.pht.sprout.cli.build

import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.lexer.Lexer
import ru.pht.sprout.module.header.parser.Parser
import ru.pht.sprout.module.header.parser.ParserException
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import ru.pht.sprout.module.repo.cache.impl.LocalCacheRepository
import ru.pht.sprout.module.repo.impl.GitflicRepository
import ru.pht.sprout.module.repo.impl.GithubRepository
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.notExists
import kotlin.io.path.readText

class BuildInfo {
    // ===== ДИРЕКТОРИИ ===== //
    val sproutDirectory = Path("${System.getProperty("user.home")}/.sprout/").absolutePathString()
    val moduleDirectory = Path(".").absolutePathString()
    // ===== РЕПОЗИТОРИИ ===== //
    val repositories: List<IRepository> = mutableListOf(GithubRepository(), GitflicRepository())
    val cachingRepository: ICachingRepository = LocalCacheRepository(this.sproutDirectory, this.repositories)
    // ===== МОДУЛЬ ===== //
    var moduleHeader: ModuleHeader? = null
        private set
    var moduleHeaderError: ParserException? = null
        private set

    // ===== API ===== //

    /**
     * Парсинг модуля.
     *
     * Записывает ошибки в [moduleHeaderError] вместо того чтобы их кидать.
     *
     * @return `true` - успешно, `false` - ошибка.
     */
    fun tryParseModule(): Boolean {
        if (this.moduleHeader != null)
            return true
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