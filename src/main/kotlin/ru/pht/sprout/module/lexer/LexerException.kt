package ru.pht.sprout.module.lexer

abstract class LexerException(message: String) : Exception(message) {
    abstract fun print(lexer: Lexer, builder: StringBuilder = StringBuilder()): StringBuilder

    class InvalidIdentifier(val identifier: String) : LexerException("Неопознанный идентификатор: $identifier") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            val (info, indent) = computeLineInfo(lexer, lexer.column - identifier.length)
            return builder
                .append(info)
                .append('\n')
                .append(indent)
                .append('^')
                .append("~".repeat(this.identifier.length - 1))
                .append(' ')
                .append(this.message)
        }
    }

    class UnexpectedSymbol(val symbol: Char) : LexerException("Неожиданный символ: $symbol") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            val (info, indent) = computeLineInfo(lexer, lexer.column)
            return builder
                .append(info)
                .append('\n')
                .append(indent)
                .append("^ ")
                .append(this.message)
        }
    }

    class UncompletedString(val start: Int) : LexerException("Незавершённая строка") {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
            val (info, indent) = computeLineInfo(lexer, this.start)
            return builder
                .append(info)
                .append('\n')
                .append(indent)
                .append("^ ")
                .append(this.message)
        }
    }

    class EOF(message: String = "Лексический анализатор достиг конца обрабатываемого источника") : LexerException(message) {
        override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder =
            builder.append(this.message)
    }

    companion object {
        fun computeLineInfo(lexer: Lexer, tokenStart: Int): Pair<String, String> {
            val (info, lineStart, lineEnd) = getLineInfo(lexer)
            val line = lexer.source.substring(lineStart, lineEnd)
            val visualLine = line.replace("\t", "    ")
            val visualTokenStart = convertTabbedPositionToVisual(line, tokenStart - lineStart)
            return Pair("$info $visualLine", " ".repeat(visualTokenStart + info.length + 1))
        }

        fun getLineInfo(lexer: Lexer): Triple<String, Int, Int> {
            val lineEnd = lexer.source.indexOf('\n', lexer.ptr)
            return Triple(
                "[${lexer.line + 1}, ${lexer.column + 1}]",
                lexer.source.lastIndexOf('\n', lexer.ptr - 1) + 1,
                if (lineEnd == -1) lexer.source.length else lineEnd
            )
        }

        private fun convertTabbedPositionToVisual(line: String, position: Int): Int {
            var visualPos = 0
            for (i in 0 until position) {
                if (i < line.length) {
                    if (line[i] == '\t') {
                        visualPos = (visualPos + 4) and (4 - 1).inv() // Округление до ближайшего, кратного 4
                    } else {
                        visualPos++
                    }
                }
            }
            return visualPos
        }
    }
}