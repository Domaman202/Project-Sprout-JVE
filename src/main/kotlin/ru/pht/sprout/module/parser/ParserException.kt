package ru.pht.sprout.module.parser

import ru.pht.sprout.module.lexer.LexerException
import ru.pht.sprout.module.lexer.LexerException.Companion.computeLineInfo
import ru.pht.sprout.module.lexer.Token

abstract class ParserException : Exception {
    abstract fun print(parser: Parser, builder: StringBuilder = StringBuilder()): StringBuilder

    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)

    class ExceptionWrapContext(
        var stage: String,
        var lastToken: Token? = null
    )

    abstract class Wrapped(val context: ExceptionWrapContext, open val exception: Throwable) : ParserException(exception) {
        protected fun StringBuilder.printHead(): StringBuilder {
            context.lastToken?.let { append('[').append(it.position.line + 1).append(", ").append(it.position.column + 1).append(']') }
            append('[').append(context.stage).append(']')
            append('\n')
            return this
        }

        class FromLexer(context: ExceptionWrapContext, override val exception: LexerException) : Wrapped(context, exception) {
            override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
                this.exception.print(parser.lexer, builder.printHead())
        }

        class FromParser(context: ExceptionWrapContext, override val exception: ParserException) : Wrapped(context, exception) {
            override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
                this.exception.print(parser, builder.printHead())
        }
    }

    class Unsupported(val token: Token?, message: String) : ParserException(message) {
        override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
            if (this.token == null)
                builder.append(this.message)
            else builder.print(parser, this.token, this.message)
    }

    class ValidationException(val token: Token?, val string: String) : ParserException("Невалидное значение '$string'") {
        override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
            if (this.token == null)
                builder.append(this.message)
            else builder.print(parser, this.token, this.message)
    }

    class NotInitializedException(val field: String) : ParserException("Неинициализированное обязательное поле '$field'") {
        override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
            builder.append(this.message)
    }

    class UnexpectedToken : ParserException {
        val accepted: Token
        val excepted: List<Token.Type>

        constructor(accepted: Token, expected: Token.Type) : super("Ожидался токен '$expected', получен токен '$accepted'") {
            this.accepted = accepted
            this.excepted = listOf(expected)
        }

        constructor(accepted: Token, expected: List<Token.Type>) : super("Ожидались токены ${expected.map { "'$it'" }}, получен токен '$accepted'") {
            this.accepted = accepted
            this.excepted = expected
        }

        override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
            builder.print(parser, this.accepted, "Неожиданный токен ${accepted.type}\nОжидались токены типа: ${excepted.map { "'$it'" }}")
    }

    companion object {
        fun StringBuilder.print(parser: Parser, token: Token, message: String?): StringBuilder {
            val (info, indent) = computeLineInfo(parser.lexer, token.position.start)
            return this
                .append(info)
                .append('\n')
                .message(indent, token.position.end - token.position.start - 1, message)
        }

        fun StringBuilder.message(indent: String, length: Int, message: String?): StringBuilder {
            append(indent)
            append('^')
            repeat(length) { append('~') }
            message ?: return this
            append(' ')
            var flag = true
            var message: String = message
            while (true) {
                if (flag)
                    flag = false
                else {
                    append('\n')
                    append(indent)
                }
                val end = message.indexOf('\n')
                if (end == -1) {
                    append(message)
                    return this
                }
                append(message.take(end))
                message = message.substring(end + 1)
            }
        }
    }
}