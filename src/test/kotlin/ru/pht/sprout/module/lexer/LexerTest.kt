package ru.pht.sprout.module.lexer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.pht.sprout.module.lexer.Token.Type.*

class LexerTest {
    @Test
    fun testInstructions() {
        val lexer = Lexer("()")
        assertEquals(INSTR_START, lexer.next().type)
        assertEquals(INSTR_END, lexer.next().type)
    }

    @Test
    fun testLists() {
        val lexer = Lexer("[]")
        assertEquals(LIST_START, lexer.next().type)
        assertEquals(LIST_END, lexer.next().type)
    }

    @Test
    fun testAttributes() {
        val lexer = Lexer("{[]}")
        assertEquals(ATTR_START, lexer.next().type)
        assertEquals(ATTR_END, lexer.next().type)
    }

    @Test
    fun testAny() {
        val lexer = Lexer("[*]")
        assertEquals(ANY, lexer.next().type)
    }

    @Test
    fun testString() {
        val lexer = Lexer("\"hello world\"")
        val token = lexer.next()
        assertEquals(STRING, token.type)
        assertEquals("hello world", token.value)
    }

    @Test
    fun testStringWithEscapes() {
        val lexer = Lexer("\"hello \\\"world\\\" \\n \\r \\t \\\\\"")
        val token = lexer.next()
        assertEquals(STRING, token.type)
        assertEquals("hello \"world\" \n \r \t \\", token.value)
    }

    @Test
    fun testUncompletedString() {
        val lexer = Lexer("\"hello")
        assertThrows(LexerException.UncompletedString::class.java) {
            lexer.next()
        }
    }

    @Test
    fun testInvalidIdentifier() {
        val lexer = Lexer("invalid-identifier!")
        assertThrows(LexerException.InvalidIdentifier::class.java) {
            lexer.next()
        }
    }
    
    @Test
    fun testUnexpectedSymbol() {
        val lexer = Lexer("{a}")
        assertThrows(LexerException.UnexpectedSymbol::class.java) {
            lexer.next()
        }
    }

    @Test
    fun testComment() {
        val lexer = Lexer("; this is a comment\n()")
        assertEquals(INSTR_START, lexer.next().type)
        assertEquals(INSTR_END, lexer.next().type)
    }

    @Test
    fun testAllIdentifiers() {
        val identifiers = mapOf(
            "module" to ID_MODULE,
            "name" to ID_NAME,
            "vers" to ID_VERSION,
            "desc" to ID_DESCRIPTION,
            "auth" to ID_AUTHORS,
            "deps" to ID_DEPENDENCIES,
            "uses" to ID_USES,
            "inject-from" to ID_INJECT_FROM,
            "inject-into" to ID_INJECT_INTO,
            "inject-into-deps" to ID_INJECT_INTO_DEPENDENCIES,
            "features" to ID_FEATURES,
            "no-features" to ID_NO_FEATURES,
            "imports" to ID_IMPORTS,
            "exports" to ID_EXPORTS,
            "src" to ID_SOURCE,
            "res" to ID_RESOURCE,
            "plg" to ID_PLUGIN,
            "default" to ID_DEFAULT,
            "optional" to ID_OPTIONAL,
            "allow" to ID_ALLOW,
            "deny" to ID_DENY,
            "plugins" to ID_PLUGINS,
            "adapters" to ID_ADAPTERS,
            "macros" to ID_MACROS,
            "types" to ID_TYPES,
            "functions" to ID_FUNCTIONS
        )
        val lexer = Lexer(identifiers.keys.joinToString(" "))
        identifiers.values.forEach {
            assertEquals(it, lexer.next().type)
        }
    }

    @Test
    fun testFullHeader() {
        val source = """
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                {[deps ["pht/core"]]}
            )
        """.trimIndent()
        val lexer = Lexer(source)
        val tokens = mutableListOf<Token.Type>()
        while (lexer.hasNext()) {
            tokens.add(lexer.next().type)
        }
        val expected = listOf(
            INSTR_START,
            ID_MODULE,
            STRING,
            ATTR_START,
            ID_NAME,
            STRING,
            ATTR_END,
            ATTR_START,
            ID_VERSION,
            STRING,
            ATTR_END,
            ATTR_START,
            ID_DEPENDENCIES,
            LIST_START,
            STRING,
            LIST_END,
            ATTR_END,
            INSTR_END
        )
        assertEquals(expected, tokens)
    }

    @Test
    fun testEOF() {
        val lexer = Lexer("")
        assertThrows(LexerException.EOF::class.java) {
            lexer.next()
        }
    }

    @Test
    fun testLexerExceptionPrints() {
        val source = "invalid!"
        val lexer = Lexer(source)
        val invalidIdentifierException = assertThrows(LexerException.InvalidIdentifier::class.java) {
            lexer.next()
        }
        assertTrue(invalidIdentifierException.print(lexer).toString().isNotBlank())

        val lexer2 = Lexer("{a")
        val unexpectedSymbolException = assertThrows(LexerException.UnexpectedSymbol::class.java) {
            lexer2.next()
        }
        assertTrue(unexpectedSymbolException.print(lexer2).toString().isNotBlank())

        val lexer3 = Lexer("\"uncompleted")
        val uncompletedStringException = assertThrows(LexerException.UncompletedString::class.java) {
            lexer3.next()
        }
        assertTrue(uncompletedStringException.print(lexer3).toString().isNotBlank())

        val lexer4 = Lexer("")
        val eofException = assertThrows(LexerException.EOF::class.java) {
            lexer4.next()
        }
        assertTrue(eofException.print(lexer4).toString().isNotBlank())
    }
}
