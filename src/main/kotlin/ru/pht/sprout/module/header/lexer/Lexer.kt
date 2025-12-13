package ru.pht.sprout.module.header.lexer

import ru.pht.sprout.module.header.lexer.Token.Type.*

/**
 * Лексический анализатор заголовков модуля.
 */
class Lexer(val source: String) : Iterator<Token> {
    var ptr: Int = 0
    var line: Int = 0
    var column: Int = 0

    /**
     * Проверка возможности токенизации нового токена.
     *
     * @return `true` - если токен есть, `false` - если токена нет.
     */
    override fun hasNext(): Boolean {
        skipWS()
        return ptr < source.length
    }

    /**
     * Токенизация следующего токена.
     *
     * @return Токен.
     * @throws LexerException.InvalidIdentifier Идентификатор не существует.
     * @throws LexerException.UnexpectedSymbol Неизвестный / неожиданный символ.
     * @throws LexerException.UncompletedString Строка не была завершена.
     * @throws LexerException.EOF Достигнут конец файла.
     */
    @Throws(LexerException::class)
    override fun next(): Token {
        skipWS()
        Snapshot(ptr, line, column).run {
            return when (val char = pop()) {
                '(' -> token(INSTR_START, "(")
                ')' -> token(INSTR_END, ")")
                '[' -> if (peek() == '*') { pop(); pop(']') { token(ANY, "*" ) } } else token(LIST_START, "[")
                ']' -> if (peek() == '}') { pop(); token(ATTR_END, "]}") } else token(LIST_END, "]")
                '{' -> { pop('[') { token(ATTR_START, "{[") } }
                '"' -> {
                    val value = StringBuilder()
                    while (ptr < source.length) {
                        when (val char = pop()) {
                            '\\' -> {
                                value.append(
                                    when (val second = pop()) {
                                        '\\' -> '\\'
                                        '\"' -> '\"'
                                        'n' -> '\n'
                                        'r' -> '\r'
                                        't' -> '\t'
                                        else -> throw LexerException.UnexpectedSymbol(second)
                                    }
                                )
                            }
                            '"' -> return token(STRING, value.toString())
                            else -> value.append(char)
                        }
                    }
                    throw LexerException.UncompletedString(this)
                }
                ';' -> {
                    while (ptr < source.length && peek() != '\n')
                        pop()
                    next()
                }
                else -> {
                    val value = StringBuilder().append(char)
                    while (ptr < source.length) {
                        val char = peek()!!
                        if (char.isLetter() || char == '-') {
                            pop()
                            value.append(char)
                        } else break
                    }
                    val string = value.toString()
                    token(
                        when (string) {
                            "module" -> ID_MODULE
                            "name" -> ID_NAME
                            "vers" -> ID_VERSION
                            "desc" -> ID_DESCRIPTION
                            "auth" -> ID_AUTHORS
                            "deps" -> ID_DEPENDENCIES
                            "uses" -> ID_USES
                            "inject-into-chain" -> INJECT_INTO_CHAIN
                            "inject-into-module" -> INJECT_INTO_MODULE
                            "no-inject-from-chain" -> NO_INJECT_FROM_CHAIN
                            "no-inject-from-module" -> NO_INJECT_FROM_MODULE
                            "features" -> ID_FEATURES
                            "no-features" -> ID_NO_FEATURES
                            "imports" -> ID_IMPORTS
                            "exports" -> ID_EXPORTS
                            "src" -> ID_SOURCE
                            "res" -> ID_RESOURCE
                            "plg" -> ID_PLUGIN
                            "default" -> ID_DEFAULT
                            "optional" -> ID_OPTIONAL
                            "allow" -> ID_ALLOW
                            "deny" -> ID_DENY
                            "plugins" -> ID_PLUGINS
                            "adapters" -> ID_ADAPTERS
                            "macros" -> ID_MACROS
                            "types" -> ID_TYPES
                            "functions" -> ID_FUNCTIONS
                            else -> {
                                if (string.length == 1) {
                                    val symbol = string.first()
                                    if (!symbol.isLetter() && symbol != '-') {
                                        throw LexerException.UnexpectedSymbol(symbol)
                                    }
                                }
                                throw LexerException.InvalidIdentifier(this, string)
                            }
                        },
                        string
                    )
                }
            }
        }
    }

    private fun Snapshot.token(type: Token.Type, value: String): Token =
        Token(Token.Position(start, ptr, startLine, startColumn), type, value)

    private fun inc() {
        if (ptr >= source.length)
            throw LexerException.EOF()
        if (source[ptr++] == '\n') {
            line++
            column = 0
        } else column++
    }

    private fun peek(): Char? =
        if (ptr < source.length)
            source[ptr]
        else null

    private fun pop(): Char {
        inc()
        return source[ptr - 1]
    }

    private inline fun <T> pop(check: Char, block: () -> T): T {
        val symbol = pop()
        if (symbol != check)
            throw LexerException.UnexpectedSymbol(symbol)
        return block()
    }

    private fun skipWS() {
        while (peek()?.isWhitespace() == true) {
            inc()
        }
    }

    data class Snapshot(val start: Int, val startLine: Int, val startColumn: Int)
}