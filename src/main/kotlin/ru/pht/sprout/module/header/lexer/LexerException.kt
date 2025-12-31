package ru.pht.sprout.module.header.lexer

import ru.DmN.translate.Language
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator
import ru.pht.sprout.utils.ErrorFormatter
import ru.pht.sprout.utils.SproutTranslate

/**
 * Исключение лексического анализатора.
 */
abstract class LexerException : Exception(), ITranslatedThrowable<LexerException> {
    abstract fun print(lexer: Lexer, language: Language, builder: StringBuilder = StringBuilder()): StringBuilder

    override val translator: ThrowableTranslator<LexerException>
        get() = SproutTranslate.ExceptionTranslator
    override val message: String
        get() = this.translate(Language.ENGLISH)

    /**
     * Идентификатор не существует.
     */
    class InvalidIdentifier(val snapshot: Lexer.Snapshot, val identifier: String) : LexerException() {
        override fun print(lexer: Lexer, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, snapshot.start, identifier.length, snapshot.startLine, snapshot.startColumn, message))
    }

    /**
     * Неизвестный / неожиданный символ.
     */
    class UnexpectedSymbol(val symbol: Char) : LexerException() {
        override fun print(lexer: Lexer, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, lexer.ptr - 1, 1, lexer.line, lexer.column - 1, message))
    }

    /**
     * Строка не была завершена.
     */
    class UncompletedString(val snapshot: Lexer.Snapshot) : LexerException() {
        override fun print(lexer: Lexer, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, snapshot.start, 1, snapshot.startLine, snapshot.startColumn, message))
    }

    /**
     * Достигнут конец файла.
     */
    class EOF : LexerException() {
        override fun print(lexer: Lexer, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(this.message)
    }
}