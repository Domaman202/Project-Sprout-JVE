package ru.pht.sprout.module.lexer

abstract class LexerException(message: String) : Exception(message) {
    protected fun printLC(lexer: Lexer): String =
        StringBuilder().append('[').append(lexer.line + 1).append(", ").append(lexer.column + 1).append(']').toString()
    abstract fun print(lexer: Lexer, builder: StringBuilder = StringBuilder()): StringBuilder

    class InvalidIdentifier(val identifier: String) : LexerException("Неопознанный идентификатор: $identifier") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            var lineStart = lexer.source.lastIndexOf('\n', lexer.ptr)
            if (lineStart == -1)
                lineStart = 0
            var lineEnd = lexer.source.indexOf('\n', lexer.ptr)
            if (lineEnd == -1)
                lineEnd = lexer.source.length
            val lc = printLC(lexer)
            return builder
                .append(lc)
                .append(lexer.source.substring(lineStart, lineEnd))
                .append('\n')
                .append(" ".repeat(lexer.column - this.identifier.length + lc.length))
                .append('^')
                .append("~".repeat(this.identifier.length - 1))
                .append(' ')
                .append(this.message)
        }
    }

    class UnexpectedSymbol(val symbol: Char) : LexerException("Неожиданный символ: $symbol") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            var lineStart = lexer.source.lastIndexOf('\n', lexer.ptr)
            if (lineStart == -1)
                lineStart = 0
            var lineEnd = lexer.source.indexOf('\n', lexer.ptr)
            if (lineEnd == -1)
                lineEnd = lexer.source.length
            val lc = printLC(lexer)
            return builder
                .append(lexer.source.substring(lineStart, lineEnd))
                .append(" ".repeat(lexer.column + lc.length - 1))
                .append("^ ")
                .append(this.message)
        }
    }

    class EOF(message: String = "Лексический анализатор достиг конца обрабатываемого источника") : LexerException(message) {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            return builder.append(this.message)
        }
    }

}