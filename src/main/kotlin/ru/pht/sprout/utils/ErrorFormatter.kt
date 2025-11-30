package ru.pht.sprout.utils

object ErrorFormatter {
    fun formatErrorWithToken(source: String, tokenStart: Int, tokenLength: Int, line: Int, column: Int, message: String): String {
        val safeTokenStart = tokenStart.coerceIn(0, source.length)
        val safeTokenLength = tokenLength.coerceAtLeast(0)

        val lineInfo = "[${line + 1}, ${column + 1}]"
        val lineStart = findLineStart(source, safeTokenStart)
        val lineEnd = findLineEnd(source, safeTokenStart)
        val originalLine = if (lineStart < lineEnd) source.substring(lineStart, lineEnd) else ""
        val visualLine = originalLine.replace("\t", "    ")
        val visualTokenStart = calculateVisualPosition(originalLine, (safeTokenStart - lineStart).coerceIn(0, originalLine.length))
        val visualTokenLength = calculateVisualLength(originalLine, safeTokenStart - lineStart, safeTokenLength)

        val builder = StringBuilder()
        builder.append(lineInfo).append(" ").append(visualLine).append("\n")

        val prefixSpaces = lineInfo.length + 1 + visualTokenStart
        repeat(prefixSpaces) { builder.append(' ') }

        builder.append('^')
        if (visualTokenLength > 1) {
            repeat(visualTokenLength - 1) { builder.append('~') }
        }

        val totalPrefixLength = prefixSpaces + visualTokenLength + 1
        appendMultilineMessage(builder, message, totalPrefixLength)

        return builder.toString()
    }

    private fun appendMultilineMessage(builder: StringBuilder, message: String, prefixLength: Int) {
        val lines = message.split('\n')
        if (lines.isEmpty()) return

        builder.append(" ").append(lines[0])

        for (i in 1 until lines.size) {
            builder.append("\n")
            repeat(prefixLength) { builder.append(' ') }
            builder.append(lines[i])
        }
    }

    private fun getVisualLineInfo(source: String, position: Int): Triple<String, String, Int> {
        val safePosition = position.coerceIn(0, source.length)
        val lineStart = findLineStart(source, safePosition)
        val lineEnd = findLineEnd(source, safePosition)
        val lineNumber = countLines(source, safePosition)

        val originalLine = if (lineStart < lineEnd) source.substring(lineStart, lineEnd) else ""
        val columnNumber = (safePosition - lineStart).coerceIn(0, originalLine.length)

        val visualLine = originalLine.replace("\t", "    ")
        val visualStart = calculateVisualPosition(originalLine, columnNumber)

        val info = "[${lineNumber + 1}, ${columnNumber + 1}]"
        return Triple(info, visualLine, visualStart)
    }

    private fun findLineStart(source: String, pos: Int): Int {
        if (source.isEmpty()) return 0
        val safePos = pos.coerceIn(0, source.length)
        if (safePos == 0) return 0

        val lastNewline = source.lastIndexOf('\n', safePos - 1)
        return if (lastNewline == -1) 0 else lastNewline + 1
    }

    private fun findLineEnd(source: String, pos: Int): Int {
        if (source.isEmpty()) return 0
        val safePos = pos.coerceIn(0, source.length)
        val nextNewline = source.indexOf('\n', safePos)
        return if (nextNewline == -1) source.length else nextNewline
    }

    private fun countLines(source: String, pos: Int): Int {
        if (source.isEmpty()) return 0
        val safePos = pos.coerceIn(0, source.length)
        var count = 0
        for (i in 0 until safePos) {
            if (source[i] == '\n') count++
        }
        return count
    }

    private fun calculateVisualPosition(line: String, pos: Int): Int {
        var visualPos = 0
        val safePos = pos.coerceIn(0, line.length)
        for (i in 0 until safePos) {
            visualPos += if (line[i] == '\t') 4 - (visualPos % 4) else 1
        }
        return visualPos
    }

    private fun calculateVisualLength(line: String, start: Int, length: Int): Int {
        if (line.isEmpty()) return 1

        val safeStart = start.coerceIn(0, line.length)
        val safeEnd = (safeStart + length).coerceIn(safeStart, line.length)

        // Если длина 0, возвращаем 1 для отображения хотя бы одного символа '^'
        if (safeStart == safeEnd) return 1

        var visualLength = 0
        for (i in safeStart until safeEnd) {
            visualLength += if (line[i] == '\t') 4 - ((visualLength + safeStart) % 4) else 1
        }
        return visualLength.coerceAtLeast(1)
    }
}