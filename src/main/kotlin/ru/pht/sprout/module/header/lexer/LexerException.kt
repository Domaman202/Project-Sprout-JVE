package ru.pht.sprout.module.header.lexer

import ru.pht.sprout.utils.fmt.ErrorFormatter

/**
 * Исключение лексического анализатора.
 */
abstract class LexerException(message: String) : Exception(message) {
    abstract fun print(lexer: Lexer, builder: StringBuilder = StringBuilder()): StringBuilder

    /**
     * Идентификатор не существует.
     */
    class InvalidIdentifier(val snapshot: Lexer.Snapshot, val identifier: String) : LexerException("Неопознанный идентификатор") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            return builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, snapshot.start, identifier.length, snapshot.startLine, snapshot.startColumn, message!!))
        }
    }

    /**
     * Неизвестный / неожиданный символ.
     */
    class UnexpectedSymbol(val symbol: Char) : LexerException("Неожиданный символ") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            return builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, lexer.ptr - 1, 1, lexer.line, lexer.column - 1, message!!))
        }
    }

    /**
     * Строка не была завершена.
     */
    class UncompletedString(val snapshot: Lexer.Snapshot) : LexerException("Незавершённая строка") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            return builder.append(ErrorFormatter.formatErrorWithToken(lexer.source, snapshot.start, 1, snapshot.startLine, snapshot.startColumn, message!!))
        }
    }

    /**
     * Достигнут конец файла.
     */
    class EOF(message: String = "Лексический анализатор достиг конца обрабатываемого источника") : LexerException(message) {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder =
            builder.append(this.message)
    }
}