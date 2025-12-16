package ru.pht.sprout.module.header.parser

import ru.pht.sprout.module.header.lexer.LexerException
import ru.pht.sprout.module.header.lexer.Token
import ru.pht.sprout.utils.fmt.ErrorFormatter
import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.SproutTranslate
import ru.pht.sprout.utils.lang.exception.TranslatedException
import ru.pht.sprout.utils.lang.Translation

abstract class ParserException : TranslatedException {
    abstract val token: Token?
    abstract fun print(parser: Parser, language: Language, builder: StringBuilder = StringBuilder()): StringBuilder

    constructor(translation: Translation) : super(translation = translation)
    constructor(cause: Throwable) : super(cause = cause, translation = null)

    class ExceptionWrapContext(
        var stage: String,
        var lastToken: Token? = null
    )

    abstract class Wrapped(val context: ExceptionWrapContext, open val exception: Throwable) : ParserException(exception) {
        override val token: Token?
            get() = context.lastToken

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

    class UnsupportedHeader(override val token: Token?, val format: String) : ParserException(SproutTranslate.of<UnsupportedHeader>()) {
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

        override fun translate(language: Language): String? =
            this.translation?.translate(language, Pair("format", this.format))
    }

    class ValidationException(override val token: Token?, val value: String) : ParserException(SproutTranslate.of<ValidationException>()) {
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

        override fun translate(language: Language): String? =
            this.translation?.translate(language, Pair("value", this.value))
    }

    class NotInitializedException(override val token: Token?, val field: String) : ParserException(SproutTranslate.of<NotInitializedException>()) {
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

        override fun translate(language: Language): String? =
            this.translation?.translate(language, Pair("field", this.field))
    }

    class UnexpectedToken(val accepted: Token, val expected: List<Token.Type>) : ParserException(SproutTranslate.of<UnexpectedToken>()) {
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
                SproutTranslate.of<UnexpectedToken>("print").translate(language, Pair("expected", this.expected.map { "'$it'" }), Pair("accepted", this.accepted.type))
            ))

        override fun translate(language: Language): String =
            this.translation!!.translate(language, Pair("expected", this.expected.map { "'$it'" }), Pair("accepted", this.accepted.type))
    }
}