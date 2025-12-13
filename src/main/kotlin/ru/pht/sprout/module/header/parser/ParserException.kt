package ru.pht.sprout.module.header.parser

import ru.pht.sprout.module.header.lexer.LexerException
import ru.pht.sprout.module.header.lexer.Token
import ru.pht.sprout.utils.fmt.ErrorFormatter

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
            context.lastToken?.let { append("[${it.position.line + 1}, ${it.position.column + 1}]") }
            append("[${context.stage}]\n")
            return this
        }

        class FromLexer(context: ExceptionWrapContext, override val exception: LexerException) : Wrapped(context, exception) {
            override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
                exception.print(parser.lexer, builder.printHead())
        }

        class FromParser(context: ExceptionWrapContext, override val exception: ParserException) : Wrapped(context, exception) {
            override fun print(parser: Parser, builder: StringBuilder): StringBuilder {
                context.lastToken?.let {
                    builder.append(ErrorFormatter.formatErrorWithToken(
                        parser.lexer.source,
                        it.position.start,
                        it.position.end - it.position.start,
                        it.position.line,
                        it.position.column,
                        ""
                    )).append('\n')
                } ?: builder.printHead()
                return exception.print(parser, builder)
            }
        }
    }

    class UnsupportedHeader(val token: Token?, message: String) : ParserException(message) {
        override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
            token?.let {
                builder.append(ErrorFormatter.formatErrorWithToken(
                    parser.lexer.source,
                    it.position.start,
                    it.position.end - it.position.start,
                    it.position.line,
                    it.position.column,
                    message!!
                ))
            } ?: builder.append(message)
    }

    class ValidationException(val token: Token?, val string: String) : ParserException("Невалидное значение '$string'") {
        override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
            token?.let {
                builder.append(ErrorFormatter.formatErrorWithToken(
                    parser.lexer.source,
                    it.position.start,
                    it.position.end - it.position.start,
                    it.position.line,
                    it.position.column,
                    message!!
                ))
            } ?: builder.append(message)
    }

    class NotInitializedException(val token: Token?, val field: String) : ParserException("Неинициализированное обязательное поле '$field'") {
        constructor(token: Token?, original: ru.pht.sprout.utils.NotInitializedException) : this(token, original.field)

        override fun print(parser: Parser, builder: StringBuilder): StringBuilder =
            token?.let {
                builder.append(ErrorFormatter.formatErrorWithToken(
                    parser.lexer.source,
                    it.position.start,
                    it.position.end - it.position.start,
                    it.position.line,
                    it.position.column,
                    message!!
                ))
            } ?: builder.append(message)
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
            builder.append(ErrorFormatter.formatErrorWithToken(
                parser.lexer.source,
                accepted.position.start,
                accepted.position.end - accepted.position.start,
                accepted.position.line,
                accepted.position.column,
                "Неожиданный токен ${accepted.type}\nОжидались токены типа: ${excepted.joinToString { "'$it'" }}"
            ))
    }
}