package ru.pht.sprout.cli.build

import kotlinx.io.IOException
import ru.DmN.translate.TranslationKey
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.lexer.Lexer
import ru.pht.sprout.module.header.parser.Parser
import ru.pht.sprout.module.header.parser.ParserException
import ru.pht.sprout.utils.exception.SproutFileNotFoundException
import ru.pht.sprout.utils.exception.SproutIllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.notExists

class ProjectInfo {
    // ===== Внутренние поля ===== //

    private var _status: Status = Status.UNINITIALIZED
    private var _workdir: Path? = null
    private var _header: ModuleHeader? = null

    // ===== Открытые поля ===== //

    val status: Status
        get() = _status
    val workdir: Path
        get() = _workdir!!
    val header: ModuleHeader
        get() = _header!!

    // ===== Открытый функционал ===== //

    /**
     * Проводит инициализацию проекта.
     *
     * @param workdir Директория проекта.
     *
     * @throws SproutIllegalStateException Проект уже был инициализирован.
     * @throws SproutFileNotFoundException Ошибка чтения файлов проекта.
     * @throws ParserException Ошибка чтения заголовка модуля проекта.
     */
    @Throws(IllegalStateException::class, IOException::class, ParserException::class)
    fun init(workdir: Path) {
        if (_status != Status.UNINITIALIZED)
            throw SproutIllegalStateException(TranslationKey.of<ProjectInfo>("alreadyInitialized"))
        val header = workdir.resolve("module.pht")
        if (header.notExists())
            throw SproutFileNotFoundException(TranslationKey.of<ProjectInfo>("headerNotFounded"), "dir" to workdir.absolute().normalize())
        _header = Parser(Lexer(Files.readAllBytes(header).toString(Charsets.UTF_8))).parse()
        _workdir = workdir.absolute().normalize()
        _status = Status.INITIALIZED
    }

    enum class Status {
        UNINITIALIZED,
        INITIALIZED,
    }

    // ===== Внутренний функционал ===== //
}