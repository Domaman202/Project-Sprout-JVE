package ru.pht.sprout.module.lexer

import ru.pht.sprout.module.lexer.Token.Type.*

class Lexer(val source: String) : Iterator<Token> {
    var ptr: Int = 0
    var line: Int = 0
    var column: Int = 0

    override fun hasNext(): Boolean {
        return ptr < source.length
    }

    @Throws(LexerException::class)
    override fun next(): Token {
        skipWS()
        val start = ptr
        return when (val char = pop()) {
            '(' -> token(INSTR_START, "(", start)
            ')' -> token(INSTR_END, ")", start)
            '[' -> if (peek() == '*') { pop(); pop(']') { token(ANY, "*", start) } } else token(LIST_START, "[", start)
            ']' -> if (peek() == '}') { pop(); token(ATTR_END, "]}", start) } else token(LIST_END, "]", start)
            '{' -> { pop('[') { token(ATTR_START, "{[", start) } }
            '"' -> {
                val column = column
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
                        '"' -> return token(STRING, value.toString(), start, column)
                        else -> value.append(char)
                    }
                }
                throw LexerException.UncompletedString(start)
            }
            ';' -> {
                while (ptr < source.length && peek() != '\n')
                    pop()
                next()
            }
            else -> {
                val column = column
                val value = StringBuilder().append(char)
                while (ptr < source.length) {
                    val char = peek()!!
                    if (char.isLetter() || char == '-') {
                        pop()
                        value.append(char)
                    } else break
                }
                val string = value.toString()
                return token(
                    when (string) {
                        "module" -> ID_MODULE
                        "name" -> ID_NAME
                        "vers" -> ID_VERSION
                        "desc" -> ID_DESCRIPTION
                        "auth" -> ID_AUTHORS
                        "deps" -> ID_DEPENDENCIES
                        "uses" -> ID_USES
                        "inject-from" -> ID_INJECT_FROM
                        "inject-into" -> ID_INJECT_INTO
                        "inject-into-deps" -> ID_INJECT_INTO_DEPENDENCIES
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
                        else -> throw LexerException.InvalidIdentifier(string)
                    },
                    string,
                    start,
                    column
                )
            }
        }
    }

    private fun token(type: Token.Type, value: String, start: Int, column: Int = this.column): Token =
        Token(Token.Position(start, this.ptr, this.line, column - 1), type, value)

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
}