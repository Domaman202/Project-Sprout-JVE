package ru.pht.sprout.module.header.parser

import ru.DmN.translate.Language
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator
import ru.pht.sprout.module.header.lexer.LexerException
import ru.pht.sprout.module.header.lexer.Token
import ru.pht.sprout.utils.ErrorFormatter
import ru.pht.sprout.utils.SproutTranslate

abstract class ParserException : Exception, ITranslatedThrowable<ParserException> {
    abstract val token: Token?
    abstract fun print(parser: Parser, language: Language, builder: StringBuilder = StringBuilder()): StringBuilder

    override val translator: ThrowableTranslator<ParserException>
        get() = SproutTranslate.ExceptionTranslator
    override val message: String?
        get() = this.translate(Language.ENGLISH)

    constructor() : super()
    constructor(cause: Throwable) : super(cause)

    class ExceptionWrapContext(
        var stage: String,
        var lastToken: Token? = null
    )

    abstract class Wrapped(val context: ExceptionWrapContext, open val exception: Throwable) : ParserException(exception) {
        override val token: Token?
            get() = context.lastToken
        override val message: String?
            get() = exception.message
        override fun translate(language: Language): String =
            if (exception is ITranslatedThrowable<*>)
                    (exception as ITranslatedThrowable<*>).translate(language)
            else message ?: ""

        class FromLexer(context: ExceptionWrapContext, override val exception: LexerException) : Wrapped(context, exception) {
            override fun print(parser: Parser, language: Language, builder: StringBuilder): StringBuilder =
                exception.print(parser.lexer, language, builder)
        }

        class FromParser(context: ExceptionWrapContext, override val exception: ParserException) : Wrapped(context, exception) {
            override fun print(parser: Parser, language: Language, builder: StringBuilder): StringBuilder {
                if (context.lastToken != exception.token) {
                    context.lastToken?.let {
                        builder.append(
                            ErrorFormatter.formatErrorWithToken(
                                parser.lexer.source,
                                it.position.start,
                                it.position.end - it.position.start,
                                it.position.line,
                                it.position.column,
                                ""
                            )
                        ).append('\n')
                    }
                }
                return exception.print(parser, language, builder)
            }
        }
    }

    class UnsupportedHeader(override val token: Token?, val format: String) : ParserException() {
        override fun print(parser: Parser, language: Language, builder: StringBuilder): StringBuilder =
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

    class ValidationException(override val token: Token?, val value: String) : ParserException() {
        override fun print(parser: Parser, language: Language, builder: StringBuilder): StringBuilder =
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

    class NotInitializedException(override val token: Token?, val field: String) : ParserException() {
        override fun print(parser: Parser, language: Language, builder: StringBuilder): StringBuilder =
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

    class UnexpectedToken(val accepted: Token, val expected: List<Token.Type>) : ParserException() {
        override val token: Token
            get() = this.accepted

        constructor(accepted: Token, expected: Token.Type) : this(accepted, listOf(expected))

        override fun print(parser: Parser, language: Language, builder: StringBuilder): StringBuilder =
            builder.append(ErrorFormatter.formatErrorWithToken(
                parser.lexer.source,
                accepted.position.start,
                accepted.position.end - accepted.position.start,
                accepted.position.line,
                accepted.position.column,
                SproutTranslate.of<UnexpectedToken>(language, "print", "expected" to this.expected.map { "'$it'" }, "accepted" to this.accepted.type)
            ))

        override val translator: ThrowableTranslator<ParserException>
            get() = Companion

        private companion object : ThrowableTranslator<ParserException>() {
            override fun translate(language: Language, throwable: ParserException): String =
                if (throwable !is UnexpectedToken)
                    SproutTranslate.ExceptionTranslator.translate(language, throwable)
                else SproutTranslate.of<UnexpectedToken>(language, "expected" to throwable.expected.map { "'$it'" }, "accepted" to throwable.accepted.type)
        }
    }
}