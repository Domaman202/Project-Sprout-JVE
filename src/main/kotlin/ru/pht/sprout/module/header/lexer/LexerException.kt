package ru.pht.sprout.module.header.lexer

import ru.pht.sprout.utils.fmt.ErrorFormatter
import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.SproutTranslate
import ru.pht.sprout.utils.lang.TranslatedException
import ru.pht.sprout.utils.lang.Translation

/**
 * Исключение лексического анализатора.
 */
abstract class LexerException(translation: Translation) : TranslatedException(translation) {
    abstract fun print(lexer: Lexer, language: Language, builder: StringBuilder = StringBuilder()): StringBuilder

    /**
     * Идентификатор не существует.
     */
    class InvalidIdentifier(val snapshot: Lexer.Snapshot, val identifier: String) : LexerException(SproutTranslate.of<InvalidIdentifier>()) {
        override fun print(lexer: Lexer, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, snapshot.start, identifier.length, snapshot.startLine, snapshot.startColumn, message!!))
    }

    /**
     * Неизвестный / неожиданный символ.
     */
    class UnexpectedSymbol(val symbol: Char) : LexerException(SproutTranslate.of<UnexpectedSymbol>()) {
        override fun print(lexer: Lexer, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, lexer.ptr - 1, 1, lexer.line, lexer.column - 1, message!!))
    }

    /**
     * Строка не была завершена.
     */
    class UncompletedString(val snapshot: Lexer.Snapshot) : LexerException(SproutTranslate.of<UncompletedString>()) {
        override fun print(lexer: Lexer, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, snapshot.start, 1, snapshot.startLine, snapshot.startColumn, message!!))
    }

    /**
     * Достигнут конец файла.
     */
    class EOF : LexerException(SproutTranslate.of<EOF>()) {
        override fun print(lexer: Lexer, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(this.message)
    }
}