package ru.pht.sprout.module.header.lexer

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import ru.pht.sprout.module.header.lexer.Token.Type.*
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#headerParseTest", disabledReason = "Тест выключен конфигурацией")
class LexerTest {
    @Test
    @DisplayName("Токенизация")
    fun identifierTokenTest() {
        val source = """
            ()
            {[]}
            []
            [*]
            ""
            module
            name
            vers version
            desc description
            auth authors
            deps dependencies
            uses
            inject-into-chain
            inject-into-module
            no-inject-from-chain
            no-inject-from-module
            features
            no-features
            imports
            exports
            src source
            res resource
            plg plugin
            default
            optional
            allow
            deny
            plugins
            adapters
            macros
            types
            functions
            ; Comment not tokenized!
        """.trimIndent()
        val expected = listOf(
            INSTR_START, INSTR_END,
            ATTR_START, ATTR_END,
            LIST_START, LIST_END,
            ANY,
            STRING,
            ID_MODULE,
            ID_NAME,
            ID_VERSION, ID_VERSION,
            ID_DESCRIPTION, ID_DESCRIPTION,
            ID_AUTHORS, ID_AUTHORS,
            ID_DEPENDENCIES, ID_DEPENDENCIES,
            ID_USES,
            ID_INJECT_INTO_CHAIN,
            ID_INJECT_INTO_MODULE,
            ID_NO_INJECT_FROM_CHAIN,
            ID_NO_INJECT_FROM_MODULE,
            ID_FEATURES,
            ID_NO_FEATURES,
            ID_IMPORTS,
            ID_EXPORTS,
            ID_SOURCE, ID_SOURCE,
            ID_RESOURCE, ID_RESOURCE,
            ID_PLUGIN, ID_PLUGIN,
            ID_DEFAULT,
            ID_OPTIONAL,
            ID_ALLOW,
            ID_DENY,
            ID_PLUGINS,
            ID_ADAPTERS,
            ID_MACROS,
            ID_TYPES,
            ID_FUNCTIONS
        )
        val lexer = Lexer(source)
        val actual = ArrayList<Token.Type>()
        while (lexer.hasNext())
            actual += lexer.next().type
        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("Токенизация строки")
    fun stringTokenTest() {
        val token = Lexer("\"Какой-то текст\\\\\\\"\\n\\r\\t\"").next()
        assertEquals(STRING, token.type)
        assertEquals("Какой-то текст\\\"\n\r\t", token.value)
    }

    @Test
    @DisplayName("Ошибка неопознанного идентификатора")
    fun invalidIdentifierThrowTest() {
        val lexer = Lexer("xxx")
        val exception = assertThrows<LexerException.InvalidIdentifier> { lexer.next() }
        assertEquals("Invalid identifier".fmt, exception.message)
        assertEquals("xxx", exception.identifier)
        assertEquals(
            """
                [1, 1] xxx
                       ^~~ Invalid identifier
            """.trimIndent().fmt,
            exception.print(lexer, Language.ENGLISH).toString()
        )
    }

    @Test
    @DisplayName("Ошибка неожиданного символа #0")
    fun unexpectedSymbolThrowTest0() {
        val lexer = Lexer("$")
        val exception = assertThrows<LexerException.UnexpectedSymbol> { lexer.next() }
        assertEquals("Unexpected symbol".fmt, exception.message)
        assertEquals('$', exception.symbol)
        assertEquals(
            """
                [1, 1] $
                       ^ Unexpected symbol
            """.trimIndent().fmt,
            exception.print(lexer, Language.ENGLISH).toString()
        )
    }

    @Test
    @DisplayName("Ошибка неожиданного символа #1")
    fun unexpectedSymbolThrowTest1() {
        val lexer = Lexer("[*)")
        val exception = assertThrows<LexerException.UnexpectedSymbol> { lexer.next() }
        assertEquals("Unexpected symbol".fmt, exception.message)
        assertEquals(')', exception.symbol)
        assertEquals(
            """
                [1, 3] [*)
                         ^ Unexpected symbol
            """.trimIndent().fmt,
            exception.print(lexer, Language.ENGLISH).toString()
        )
    }

    @Test
    @DisplayName("Ошибка неожиданного символа в строке")
    fun unexpectedSymbolInStringThrowTest() {
        val lexer = Lexer("\"\\$\"")
        val exception = assertThrows<LexerException.UnexpectedSymbol> { lexer.next() }
        assertEquals("Unexpected symbol".fmt, exception.message)
        assertEquals('$', exception.symbol)
        assertEquals(
            """
                [1, 3] "\$"
                         ^ Unexpected symbol
            """.trimIndent().fmt,
            exception.print(lexer, Language.ENGLISH).toString()
        )
    }

    @Test
    @DisplayName("Ошибка незавершённой строки")
    fun uncompletedStringThrowTest() {
        val lexer = Lexer("\"")
        val exception = assertThrows<LexerException.UncompletedString> { lexer.next() }
        assertEquals("Uncompleted string".fmt, exception.message)
        assertEquals(
            """
                [1, 1] "
                       ^ Uncompleted string
            """.trimIndent().fmt,
            exception.print(lexer, Language.ENGLISH).toString()
        )
    }


    @Test
    @DisplayName("Ошибка конца файла")
    fun eofThrowTest() {
        val lexer = Lexer("")
        val exception = assertThrows<LexerException.EOF> { lexer.next() }
        assertEquals("The lexical analyzer has reached the end of the source being processed".fmt, exception.message)
        assertEquals(
            "The lexical analyzer has reached the end of the source being processed".fmt,
            exception.print(lexer, Language.ENGLISH).toString()
        )
    }
}