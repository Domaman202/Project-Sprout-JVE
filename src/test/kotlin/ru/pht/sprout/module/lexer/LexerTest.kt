package ru.pht.sprout.module.lexer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows

class LexerTest {

    @Nested
    @DisplayName("Single character tokens")
    inner class SingleCharTokens {
        @Test
        fun `should tokenize instruction start`() {
            val lexer = Lexer("(")
            val token = lexer.next()
            assertEquals(Token.Type.INSTR_START, token.type)
            assertEquals("(", token.value)
        }

        @Test
        fun `should tokenize instruction end`() {
            val lexer = Lexer(")")
            val token = lexer.next()
            assertEquals(Token.Type.INSTR_END, token.type)
            assertEquals(")", token.value)
        }

        @Test
        fun `should tokenize list start`() {
            val lexer = Lexer("[")
            val token = lexer.next()
            assertEquals(Token.Type.LIST_START, token.type)
            assertEquals("[", token.value)
        }

        @Test
        fun `should tokenize list end`() {
            val lexer = Lexer("]")
            val token = lexer.next()
            assertEquals(Token.Type.LIST_END, token.type)
            assertEquals("]", token.value)
        }
    }

    @Nested
    @DisplayName("Multi-character tokens")
    inner class MultiCharTokens {
        @Test
        fun `should tokenize any token`() {
            val lexer = Lexer("[*]")
            val token = lexer.next()
            assertEquals(Token.Type.ANY, token.type)
            assertEquals("*", token.value)
        }

        @Test
        fun `should tokenize attr start`() {
            val lexer = Lexer("{[")
            val token = lexer.next()
            assertEquals(Token.Type.ATTR_START, token.type)
            assertEquals("{[", token.value)
        }

        @Test
        fun `should tokenize attr end`() {
            val lexer = Lexer("]}")
            val token = lexer.next()
            assertEquals(Token.Type.ATTR_END, token.type)
            assertEquals("]}", token.value)
        }

        @Test
        fun `should handle list end separately from attr end`() {
            val lexer = Lexer("] ]}")
            val token1 = lexer.next()
            val token2 = lexer.next()

            assertEquals(Token.Type.LIST_END, token1.type)
            assertEquals(Token.Type.ATTR_END, token2.type)
        }
    }

    @Nested
    @DisplayName("String tokens")
    inner class StringTokens {
        @Test
        fun `should tokenize simple string`() {
            val lexer = Lexer("\"hello\"")
            val token = lexer.next()
            assertEquals(Token.Type.STRING, token.type)
            assertEquals("hello", token.value)
        }

        @Test
        fun `should tokenize empty string`() {
            val lexer = Lexer("\"\"")
            val token = lexer.next()
            assertEquals(Token.Type.STRING, token.type)
            assertEquals("", token.value)
        }

        @Test
        fun `should tokenize string with escape sequences`() {
            val lexer = Lexer("\"line1\\nline2\\ttab\\\"quote\\\\backslash\"")
            val token = lexer.next()
            assertEquals(Token.Type.STRING, token.type)
            assertEquals("line1\nline2\ttab\"quote\\backslash", token.value)
        }

        @Test
        fun `should tokenize string with slashes`() {
            val lexer = Lexer("\"pht/module\"")
            val token = lexer.next()
            assertEquals(Token.Type.STRING, token.type)
            assertEquals("pht/module", token.value)
        }

        @Test
        fun `should throw on unclosed string`() {
            val lexer = Lexer("\"unclosed string")
            assertThrows<LexerException.UncompletedString> {
                lexer.next()
            }
        }

        @Test
        fun `should throw on invalid escape sequence`() {
            val lexer = Lexer("\"invalid\\escape\"")
            assertThrows<LexerException.UnexpectedSymbol> {
                lexer.next()
            }
        }
    }

    @Nested
    @DisplayName("Comment handling")
    inner class CommentHandling {
        @Test
        fun `should skip single line comment`() {
            val lexer = Lexer("; comment\n(")
            val token = lexer.next()
            assertEquals(Token.Type.INSTR_START, token.type)
        }

        @Test
        fun `should skip comment at end of file`() {
            val lexer = Lexer("; comment")
            assertThrows<LexerException.EOF> {
                lexer.next()
            }
        }

        @Test
        fun `should skip multiple comments`() {
            val lexer = Lexer("; first comment\n; second comment\nmodule")
            val token = lexer.next()
            assertEquals(Token.Type.ID_MODULE, token.type)
        }

        @Test
        fun `should handle comment with special characters`() {
            val lexer = Lexer("; Comment with [*] {[ ]} \"quotes\"\nname")
            val token = lexer.next()
            assertEquals(Token.Type.ID_NAME, token.type)
        }

        @Test
        fun `should skip comment until newline`() {
            val lexer = Lexer("; comment ( should be skipped\n)")
            val token = lexer.next()
            assertEquals(Token.Type.INSTR_END, token.type)
        }

        @Test
        fun `should handle comment followed by EOF`() {
            val lexer = Lexer("; comment")
            assertThrows<LexerException.EOF> {
                lexer.next()
            }
        }
    }

    @Nested
    @DisplayName("Identifier tokens")
    inner class IdentifierTokens {
        @Test
        fun `should tokenize module identifier`() {
            val lexer = Lexer("module")
            val token = lexer.next()
            assertEquals(Token.Type.ID_MODULE, token.type)
            assertEquals("module", token.value)
        }

        @Test
        fun `should tokenize name identifier`() {
            val lexer = Lexer("name")
            val token = lexer.next()
            assertEquals(Token.Type.ID_NAME, token.type)
            assertEquals("name", token.value)
        }

        @Test
        fun `should tokenize version identifier`() {
            val lexer = Lexer("vers")
            val token = lexer.next()
            assertEquals(Token.Type.ID_VERSION, token.type)
            assertEquals("vers", token.value)
        }

        @Test
        fun `should tokenize description identifier`() {
            val lexer = Lexer("desc")
            val token = lexer.next()
            assertEquals(Token.Type.ID_DESCRIPTION, token.type)
            assertEquals("desc", token.value)
        }

        @Test
        fun `should tokenize authors identifier`() {
            val lexer = Lexer("auth")
            val token = lexer.next()
            assertEquals(Token.Type.ID_AUTHORS, token.type)
            assertEquals("auth", token.value)
        }

        @Test
        fun `should tokenize all supported identifiers`() {
            val identifiers = mapOf(
                "module" to Token.Type.ID_MODULE,
                "name" to Token.Type.ID_NAME,
                "vers" to Token.Type.ID_VERSION,
                "desc" to Token.Type.ID_DESCRIPTION,
                "auth" to Token.Type.ID_AUTHORS,
                "deps" to Token.Type.ID_DEPENDENCIES,
                "uses" to Token.Type.ID_USES,
                "inject-from" to Token.Type.ID_INJECT_FROM,
                "inject-into" to Token.Type.ID_INJECT_INTO,
                "inject-into-deps" to Token.Type.ID_INJECT_INTO_DEPENDENCIES,
                "features" to Token.Type.ID_FEATURES,
                "no-features" to Token.Type.ID_NO_FEATURES,
                "imports" to Token.Type.ID_IMPORTS,
                "exports" to Token.Type.ID_EXPORTS,
                "src" to Token.Type.ID_SOURCE,
                "res" to Token.Type.ID_RESOURCE,
                "plg" to Token.Type.ID_PLUGIN,
                "default" to Token.Type.ID_DEFAULT,
                "optional" to Token.Type.ID_OPTIONAL,
                "allow" to Token.Type.ID_ALLOW,
                "deny" to Token.Type.ID_DENY,
                "plugins" to Token.Type.ID_PLUGINS,
                "adapters" to Token.Type.ID_ADAPTERS,
                "macros" to Token.Type.ID_MACROS,
                "types" to Token.Type.ID_TYPES,
                "functions" to Token.Type.ID_FUNCTIONS
            )

            identifiers.forEach { (identifier, expectedType) ->
                val lexer = Lexer(identifier)
                val token = lexer.next()
                assertEquals(expectedType, token.type, "Failed for identifier: $identifier")
                assertEquals(identifier, token.value)
            }
        }

        @Test
        fun `should throw on invalid identifier`() {
            val lexer = Lexer("invalid")
            assertThrows<LexerException.InvalidIdentifier> {
                lexer.next()
            }
        }

        @Test
        fun `should handle identifiers without trailing whitespace`() {
            val lexer = Lexer("module")
            val token = lexer.next()
            assertEquals(Token.Type.ID_MODULE, token.type)
            assertEquals("module", token.value)
        }

        @Test
        fun `should handle identifiers with hyphens`() {
            val lexer = Lexer("inject-from inject-into inject-into-deps no-features")
            val token1 = lexer.next()
            val token2 = lexer.next()
            val token3 = lexer.next()
            val token4 = lexer.next()

            assertEquals(Token.Type.ID_INJECT_FROM, token1.type)
            assertEquals("inject-from", token1.value)
            assertEquals(Token.Type.ID_INJECT_INTO, token2.type)
            assertEquals("inject-into", token2.value)
            assertEquals(Token.Type.ID_INJECT_INTO_DEPENDENCIES, token3.type)
            assertEquals("inject-into-deps", token3.value)
            assertEquals(Token.Type.ID_NO_FEATURES, token4.type)
            assertEquals("no-features", token4.value)
        }
    }

    @Nested
    @DisplayName("Whitespace handling")
    inner class WhitespaceHandling {
        @Test
        fun `should skip leading whitespace`() {
            val lexer = Lexer("   \t\n  (")
            val token = lexer.next()
            assertEquals(Token.Type.INSTR_START, token.type)
        }

        @Test
        fun `should skip whitespace between tokens`() {
            val lexer = Lexer("(   )")
            val token1 = lexer.next()
            val token2 = lexer.next()

            assertEquals(Token.Type.INSTR_START, token1.type)
            assertEquals(Token.Type.INSTR_END, token2.type)
        }

        @Test
        fun `should handle multiple tokens with whitespace`() {
            val lexer = Lexer("( ) [ ]")
            val tokens = mutableListOf<Token>()
            repeat(4) {
                tokens.add(lexer.next())
            }

            val expectedTypes = listOf(
                Token.Type.INSTR_START,
                Token.Type.INSTR_END,
                Token.Type.LIST_START,
                Token.Type.LIST_END
            )

            expectedTypes.forEachIndexed { index, expectedType ->
                assertEquals(expectedType, tokens[index].type)
            }
        }
    }

    @Nested
    @DisplayName("Complex sequences")
    inner class ComplexSequences {
        @Test
        fun `should tokenize complete module example`() {
            val input = """
                (module "pht/module"
                    ; Имя и версия модуля
                    {[name "pht/example"]}
                    {[vers "1.0.0"]}
                    
                    ; Описание и авторы
                    {[desc "Пример модуля Пихты"]}
                    {[auth ["DomamaN202" "Phantom"]]}
                    
                    ; Зависимости (автоматически)
                    {[deps ["pht/core"]]}
                    {[uses ["pht/core"]]})
            """.trimIndent()

            val lexer = Lexer(input)
            val tokens = mutableListOf<Token>()
            while (true) {
                try {
                    tokens.add(lexer.next())
                } catch (_: LexerException.EOF) {
                    break
                }
            }

            val expectedTypes = listOf(
                Token.Type.INSTR_START,
                Token.Type.ID_MODULE,
                Token.Type.STRING,
                Token.Type.ATTR_START,
                Token.Type.ID_NAME,
                Token.Type.STRING,
                Token.Type.ATTR_END,
                Token.Type.ATTR_START,
                Token.Type.ID_VERSION,
                Token.Type.STRING,
                Token.Type.ATTR_END,
                Token.Type.ATTR_START,
                Token.Type.ID_DESCRIPTION,
                Token.Type.STRING,
                Token.Type.ATTR_END,
                Token.Type.ATTR_START,
                Token.Type.ID_AUTHORS,
                Token.Type.LIST_START,
                Token.Type.STRING,
                Token.Type.STRING,
                Token.Type.LIST_END,
                Token.Type.ATTR_END,
                Token.Type.ATTR_START,
                Token.Type.ID_DEPENDENCIES,
                Token.Type.LIST_START,
                Token.Type.STRING,
                Token.Type.LIST_END,
                Token.Type.ATTR_END,
                Token.Type.ATTR_START,
                Token.Type.ID_USES,
                Token.Type.LIST_START,
                Token.Type.STRING,
                Token.Type.LIST_END,
                Token.Type.ATTR_END,
                Token.Type.INSTR_END
            )

            assertEquals(expectedTypes.size, tokens.size)
            expectedTypes.forEachIndexed { index, expectedType ->
                assertEquals(expectedType, tokens[index].type, "Token at index $index")
            }
        }

        @Test
        fun `should tokenize multiple tokens`() {
            val lexer = Lexer("( ) [ ] {[ ]} [*] \"test\" module")
            val tokens = mutableListOf<Token>()
            while (true) {
                try {
                    tokens.add(lexer.next())
                } catch (_: LexerException.EOF) {
                    break
                }
            }

            val expectedTypes = listOf(
                Token.Type.INSTR_START,
                Token.Type.INSTR_END,
                Token.Type.LIST_START,
                Token.Type.LIST_END,
                Token.Type.ATTR_START,
                Token.Type.ATTR_END,
                Token.Type.ANY,
                Token.Type.STRING,
                Token.Type.ID_MODULE
            )

            assertEquals(expectedTypes.size, tokens.size)
            expectedTypes.forEachIndexed { index, expectedType ->
                assertEquals(expectedType, tokens[index].type)
            }
        }

        @Test
        fun `should handle empty input`() {
            val lexer = Lexer("")
            assertThrows<LexerException.EOF> {
                lexer.next()
            }
        }

        @Test
        fun `should track positions correctly`() {
            val lexer = Lexer("(\n \"test\"\n module )")
            val token1 = lexer.next() // (
            val token2 = lexer.next() // "test"
            val token3 = lexer.next() // module
            val token4 = lexer.next() // )

            // Instruction start
            assertEquals(0, token1.position.start)
            assertEquals(1, token1.position.end)
            assertEquals(0, token1.position.line)
            assertEquals(0, token1.position.column)

            // String token
            assertEquals(3, token2.position.start)
            assertEquals(9, token2.position.end)
            assertEquals(1, token2.position.line)
            assertEquals(1, token2.position.column)

            // Identifier
            assertEquals(11, token3.position.start)
            assertEquals(17, token3.position.end)
            assertEquals(2, token3.position.line)
            assertEquals(1, token3.position.column)

            // Instruction end
            assertEquals(18, token4.position.start)
            assertEquals(19, token4.position.end)
            assertEquals(2, token4.position.line)
            assertEquals(8, token4.position.column)
        }

        @Test
        fun `should handle identifiers at end of input`() {
            val lexer = Lexer("module")
            val token = lexer.next()
            assertEquals(Token.Type.ID_MODULE, token.type)
            assertEquals("module", token.value)
        }
    }

    @Nested
    @DisplayName("Error cases")
    inner class ErrorCases {
        @Test
        fun `should throw on unexpected symbol in multi-char token`() {
            val lexer = Lexer("{]")
            assertThrows<LexerException.UnexpectedSymbol> {
                lexer.next()
            }
        }

        @Test
        fun `should throw on incomplete any token`() {
            val lexer = Lexer("[*")
            assertThrows<LexerException.EOF> {
                lexer.next()
            }
        }

        @Test
        fun `should throw on wrong any token end`() {
            val lexer = Lexer("[*x")
            assertThrows<LexerException.UnexpectedSymbol> {
                lexer.next()
            }
        }

        @Test
        fun `should throw on EOF during identifier`() {
            val lexer = Lexer("modul")
            assertThrows<LexerException.InvalidIdentifier> {
                lexer.next()
            }
        }

        @Test
        fun `should throw on EOF during string`() {
            val lexer = Lexer("\"unclosed")
            assertThrows<LexerException.UncompletedString> {
                lexer.next()
            }
        }

        @Test
        fun `should throw on unexpected character`() {
            val lexer = Lexer("@")
            assertThrows<LexerException.InvalidIdentifier> {
                lexer.next()
            }
        }

        @Test
        fun `should throw on invalid character after identifier`() {
            val lexer = Lexer("module@")
            val token1 = lexer.next()
            assertEquals(Token.Type.ID_MODULE, token1.type)
            assertThrows<LexerException.InvalidIdentifier> {
                lexer.next()
            }
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCases {
        @Test
        fun `should handle only whitespace input`() {
            val lexer = Lexer("   \t\n  ")
            assertThrows<LexerException.EOF> {
                lexer.next()
            }
        }

        @Test
        fun `should handle mixed identifiers and symbols without spaces`() {
            val lexer = Lexer("module(name\"test\")")
            val tokens = mutableListOf<Token>()
            repeat(5) {
                tokens.add(lexer.next())
            }

            assertEquals(Token.Type.ID_MODULE, tokens[0].type)
            assertEquals(Token.Type.INSTR_START, tokens[1].type)
            assertEquals(Token.Type.ID_NAME, tokens[2].type)
            assertEquals(Token.Type.STRING, tokens[3].type)
            assertEquals(Token.Type.INSTR_END, tokens[4].type)
        }

        @Test
        fun `should handle complex attribute structure`() {
            val lexer = Lexer("{[ ]} [*]")
            val tokens = mutableListOf<Token>()
            repeat(3) {
                tokens.add(lexer.next())
            }

            assertEquals(Token.Type.ATTR_START, tokens[0].type)
            assertEquals(Token.Type.ATTR_END, tokens[1].type)
            assertEquals(Token.Type.ANY, tokens[2].type)
        }

        @Test
        fun `should handle identifiers followed by EOF`() {
            val lexer = Lexer("module name")
            val token1 = lexer.next()
            val token2 = lexer.next()

            assertEquals(Token.Type.ID_MODULE, token1.type)
            assertEquals(Token.Type.ID_NAME, token2.type)

            assertThrows<LexerException.EOF> {
                lexer.next()
            }
        }
    }

    @Nested
    @DisplayName("Identifier boundary cases")
    inner class IdentifierBoundaryCases {
        @Test
        fun `should handle identifier ending at EOF`() {
            val lexer = Lexer("module")
            val token = lexer.next()
            assertEquals(Token.Type.ID_MODULE, token.type)

            assertThrows<LexerException.EOF> {
                lexer.next()
            }
        }

        @Test
        fun `should handle identifier followed by symbol without space`() {
            val lexer = Lexer("module(")
            val token1 = lexer.next()
            val token2 = lexer.next()

            assertEquals(Token.Type.ID_MODULE, token1.type)
            assertEquals(Token.Type.INSTR_START, token2.type)
        }

        @Test
        fun `should break identifier at whitespace`() {
            val lexer = Lexer("module name")
            val token1 = lexer.next()
            val token2 = lexer.next()

            assertEquals(Token.Type.ID_MODULE, token1.type)
            assertEquals("module", token1.value)
            assertEquals(Token.Type.ID_NAME, token2.type)
            assertEquals("name", token2.value)
        }

        @Test
        fun `should handle identifier with numbers`() {
            val lexer = Lexer("module123")
            val token = lexer.next()
            assertEquals(Token.Type.ID_MODULE, token.type)
            assertEquals("module", token.value)
        }
    }

    @Nested
    @DisplayName("Real-world scenarios")
    inner class RealWorldScenarios {
        @Test
        fun `should handle complex module definition`() {
            val input = """
                (module "pht/module"
                    {[name "pht/example"]}
                    {[vers "1.0.0"]}
                    {[desc "Пример модуля Пихты"]}
                    {[auth ["DomamaN202" "Phantom"]]}
                    {[deps ["pht/core"]]}
                    {[uses ["pht/core"]]}
                    {[imports [adapters types functions]]}
                    {[exports [plugins macros]]}
                    {[features [["log" default] ["fast" optional]]]})
            """.trimIndent()

            val lexer = Lexer(input)
            val tokens = mutableListOf<Token>()
            while (true) {
                try {
                    tokens.add(lexer.next())
                } catch (_: LexerException.EOF) {
                    break
                }
            }

            // Just verify we can tokenize the entire input without exceptions
            assertTrue(tokens.isNotEmpty())
            // Check a few key tokens
            assertEquals(Token.Type.ID_MODULE, tokens[1].type)
            assertEquals(Token.Type.STRING, tokens[2].type)
            assertEquals("pht/module", tokens[2].value)
        }

        @Test
        fun `should handle nested structures`() {
            val input = """
                {[deps [
                    (module
                        {[name "pht/math"]}
                        {[vers [*]]}
                        {[uses allow]}
                        {[adapters []]})
                ]]}
            """.trimIndent()

            val lexer = Lexer(input)
            val tokens = mutableListOf<Token>()
            while (true) {
                try {
                    tokens.add(lexer.next())
                } catch (_: LexerException.EOF) {
                    break
                }
            }

            // Verify we can tokenize nested structures
            assertTrue(tokens.isNotEmpty())
            // Check key tokens
            assertEquals(Token.Type.ID_DEPENDENCIES, tokens[1].type)
            assertEquals(Token.Type.INSTR_START, tokens[3].type)
            assertEquals(Token.Type.ID_MODULE, tokens[4].type)
        }
    }
}