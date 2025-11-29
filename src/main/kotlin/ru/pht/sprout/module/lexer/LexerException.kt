package ru.pht.sprout.module.lexer

import ru.pht.sprout.utils.ErrorFormatter

abstract class LexerException(message: String) : Exception(message) {
    abstract fun print(lexer: Lexer, builder: StringBuilder = StringBuilder()): StringBuilder

    class InvalidIdentifier(val identifier: String) : LexerException("Неопознанный идентификатор: $identifier") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            val startPos = lexer.ptr - identifier.length
            return builder.append(ErrorFormatter.formatError(lexer.source, startPos, identifier.length, message!!))
        }
    }

    class UnexpectedSymbol(val symbol: Char) : LexerException("Неожиданный символ: $symbol") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            return builder.append(ErrorFormatter.formatError(lexer.source, lexer.ptr - 1, 1, message!!))
        }
    }

    class UncompletedString(val start: Int) : LexerException("Незавершённая строка") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            return builder.append(ErrorFormatter.formatError(lexer.source, this.start, 1, message!!))
        }
    }

    class EOF(message: String = "Лексический анализатор достиг конца обрабатываемого источника") : LexerException(message) {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder = builder.append(message)
    }
}