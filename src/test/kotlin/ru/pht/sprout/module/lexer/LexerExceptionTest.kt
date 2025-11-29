package ru.pht.sprout.module.lexer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.Modifier

class LexerExceptionTest {

    @Nested
    @DisplayName("LexerException base class")
    inner class LexerExceptionBaseTest {
        @Test
        fun `should be abstract class`() {
            val modifiers = LexerException::class.java.modifiers
            assertTrue(Modifier.isAbstract(modifiers))
        }

        @Test
        fun `should have message in constructor`() {
            val exception = object : LexerException("Test message") {
                override fun print(lexer: Lexer, builder: StringBuilder): StringBuilder {
                    return builder.append("test")
                }
            }
            assertEquals("Test message", exception.message)
        }

        @Test
        fun `getLineInfo should return correct line info`() {
            val source = "line1\nline2\nline3"
            val lexer = Lexer(source)

            // Устанавливаем указатель на начало второй строки
            lexer.ptr = 7 // позиция 'l' в "line2"
            lexer.line = 1
            lexer.column = 0

            val (location, lineStart, lineEnd) = LexerException.getLineInfo(lexer)

            assertEquals("[2, 1]", location) // line+1, column+1
            assertEquals(6, lineStart) // начало второй строки (после \n)
            assertEquals(11, lineEnd) // конец второй строки (перед \n)
        }

        @Test
        fun `getLineInfo should handle first line correctly`() {
            val source = "single line"
            val lexer = Lexer(source)
            lexer.ptr = 3
            lexer.line = 0
            lexer.column = 3

            val (location, lineStart, lineEnd) = LexerException.getLineInfo(lexer)

            assertEquals("[1, 4]", location)
            assertEquals(0, lineStart)
            assertEquals(source.length, lineEnd)
        }

        @Test
        fun `getLineInfo should handle last line correctly`() {
            val source = "line1\nline2"
            val lexer = Lexer(source)
            lexer.ptr = 9 // позиция 'e' в "line2"
            lexer.line = 1
            lexer.column = 3

            val (location, lineStart, lineEnd) = LexerException.getLineInfo(lexer)

            assertEquals("[2, 4]", location)
            assertEquals(6, lineStart)
            assertEquals(source.length, lineEnd) // нет \n в конце
        }
    }

    @Nested
    @DisplayName("InvalidIdentifier exception")
    inner class InvalidIdentifierTest {
        @Test
        fun `should create with identifier`() {
            val exception = LexerException.InvalidIdentifier("invalid")
            assertEquals("invalid", exception.identifier)
            assertEquals("Неопознанный идентификатор: invalid", exception.message)
        }

        @Test
        fun `should print error message with identifier highlighting`() {
            val source = "module invalid name"
            val lexer = Lexer(source)

            // Simulate position after reading "invalid"
            lexer.ptr = 13 // position after "invalid "
            lexer.line = 0
            lexer.column = 13

            val exception = LexerException.InvalidIdentifier("invalid")
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("Неопознанный идентификатор: invalid"))
            assertTrue(result.contains("module invalid name"))
            assertTrue(result.contains("^~~~~~~")) // Should highlight "invalid"
        }

        @Test
        fun `should handle identifier at start of line`() {
            val source = "invalid name"
            val lexer = Lexer(source)

            lexer.ptr = 7 // position after "invalid"
            lexer.line = 0
            lexer.column = 7

            val exception = LexerException.InvalidIdentifier("invalid")
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("invalid name"))
            assertTrue(result.contains("^~~~~~~"))
        }

        @Test
        fun `should handle identifier at end of line`() {
            val source = "name invalid"
            val lexer = Lexer(source)

            lexer.ptr = 12 // position after "invalid"
            lexer.line = 0
            lexer.column = 12

            val exception = LexerException.InvalidIdentifier("invalid")
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("name invalid"))
            assertTrue(result.contains("     ^~~~~~~"))
        }

        @Test
        fun `should handle multi-line source`() {
            val source = "module\ninvalid name\nversion"
            val lexer = Lexer(source)

            // Position at second line after "invalid"
            lexer.ptr = 14 // position after "invalid" on second line
            lexer.line = 1
            lexer.column = 7

            val exception = LexerException.InvalidIdentifier("invalid")
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("invalid name")) // Should only show current line
            assertTrue(result.contains("^~~~~~~"))
            assertFalse(result.contains("module")) // Should not show previous line
            assertFalse(result.contains("version")) // Should not show next line
        }

        @Test
        fun `should handle single character identifier`() {
            val source = "x name"
            val lexer = Lexer(source)

            lexer.ptr = 1 // position after "x"
            lexer.line = 0
            lexer.column = 1

            val exception = LexerException.InvalidIdentifier("x")
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("x name"))
            assertTrue(result.contains("^")) // Single character, no tildes
            assertFalse(result.contains("~")) // No tildes for single char
        }

        @Test
        fun `should handle empty source`() {
            val source = ""
            val lexer = Lexer(source)

            lexer.ptr = 0
            lexer.line = 0
            lexer.column = 0

            val exception = LexerException.InvalidIdentifier("test")
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("Неопознанный идентификатор: test"))
        }
    }

    @Nested
    @DisplayName("UnexpectedSymbol exception")
    inner class UnexpectedSymbolTest {
        @Test
        fun `should create with symbol`() {
            val exception = LexerException.UnexpectedSymbol('@')
            assertEquals('@', exception.symbol)
            assertEquals("Неожиданный символ: @", exception.message)
        }

        @Test
        fun `should print error message with symbol highlighting`() {
            val source = "module @ name"
            val lexer = Lexer(source)

            // Position at unexpected symbol
            lexer.ptr = 7 // position of '@'
            lexer.line = 0
            lexer.column = 7

            val exception = LexerException.UnexpectedSymbol('@')
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("Неожиданный символ: @"))
            assertTrue(result.contains("module @ name"))
            assertTrue(result.contains("^")) // Should point to '@'
        }

        @Test
        fun `should handle symbol at start of line`() {
            val source = "@ name"
            val lexer = Lexer(source)

            lexer.ptr = 1 // position after '@'
            lexer.line = 0
            lexer.column = 1

            val exception = LexerException.UnexpectedSymbol('@')
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("@ name"))
            assertTrue(result.contains("^"))
        }

        @Test
        fun `should handle symbol at end of line`() {
            val source = "name @"
            val lexer = Lexer(source)

            lexer.ptr = 6 // position after '@'
            lexer.line = 0
            lexer.column = 6

            val exception = LexerException.UnexpectedSymbol('@')
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("name @"))
            assertTrue(result.contains("     ^"))
        }

        @Test
        fun `should handle whitespace symbol`() {
            val source = "module\tname"
            val lexer = Lexer(source)

            lexer.ptr = 7 // position after tab
            lexer.line = 0
            lexer.column = 7

            val exception = LexerException.UnexpectedSymbol('\t')
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("Неожиданный символ: \t"))
        }

        @Test
        fun `should handle unprintable symbols`() {
            val source = "module${0.toChar()}name"
            val lexer = Lexer(source)

            lexer.ptr = 7 // position after null character
            lexer.line = 0
            lexer.column = 7

            val exception = LexerException.UnexpectedSymbol(0.toChar())
            val result = exception.print(lexer).toString()

            assertTrue(result.contains("Неожиданный символ: "))
        }
    }

    @Nested
    @DisplayName("EOF exception")
    inner class EOFTest {
        @Test
        fun `should create with default message`() {
            val exception = LexerException.EOF()
            assertEquals("Лексический анализатор достиг конца обрабатываемого источника", exception.message)
        }

        @Test
        fun `should create with custom message`() {
            val exception = LexerException.EOF("Custom EOF message")
            assertEquals("Custom EOF message", exception.message)
        }

        @Test
        fun `should print only the message`() {
            val lexer = Lexer("test")
            lexer.ptr = 4 // end of source
            lexer.line = 0
            lexer.column = 4

            val exception = LexerException.EOF()
            val result = exception.print(lexer).toString()

            assertEquals("Лексический анализатор достиг конца обрабатываемого источника", result)
        }

        @Test
        fun `should print custom message`() {
            val lexer = Lexer("test")
            lexer.ptr = 4

            val exception = LexerException.EOF("Unexpected end of file")
            val result = exception.print(lexer).toString()

            assertEquals("Unexpected end of file", result)
        }
    }

    @Nested
    @DisplayName("Integration tests")
    inner class IntegrationTests {
        @Test
        fun `should throw InvalidIdentifier for unknown identifier`() {
            val lexer = Lexer("unknown")

            val exception = assertThrows(LexerException.InvalidIdentifier::class.java) {
                lexer.next()
            }

            assertEquals("unknown", exception.identifier)
        }

        @Test
        fun `should throw InvalidIdentifier for invalid character`() {
            val lexer = Lexer("@")

            assertThrows(LexerException.InvalidIdentifier::class.java) {
                lexer.next()
            }
        }

        @Test
        fun `should throw EOF for empty input`() {
            val lexer = Lexer("")

            assertThrows(LexerException.EOF::class.java) {
                lexer.next()
            }
        }

        @Test
        fun `should print readable error messages in real scenario`() {
            val source = "module invalid123 name"
            val lexer = Lexer(source)

            // Read first token
            val firstToken = lexer.next()
            assertEquals(Token.Type.ID_MODULE, firstToken.type)

            // Next token should throw InvalidIdentifier
            val exception = assertThrows(LexerException.InvalidIdentifier::class.java) {
                lexer.next()
            }

            val errorMessage = exception.print(lexer).toString()

            assertTrue(errorMessage.contains("invalid123"))
            assertTrue(errorMessage.contains("Неопознанный идентификатор"))
            assertTrue(errorMessage.contains("^")) // Pointer
            assertTrue(errorMessage.contains("~")) // Underline
        }

        @Test
        fun `exception messages should not contain stack trace in normal flow`() {
            val outputStream = ByteArrayOutputStream()
            val printStream = PrintStream(outputStream)
            val oldErr = System.err

            try {
                System.setErr(printStream)

                val lexer = Lexer("invalid")
                assertThrows(LexerException.InvalidIdentifier::class.java) {
                    lexer.next()
                }

                val output = outputStream.toString()
                // Should not print stack trace for expected exceptions
                assertFalse(output.contains("at ru.pht.sprout.module.lexer.Lexer"))
            } finally {
                System.setErr(oldErr)
            }
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCases {
        @Test
        fun `should handle very long lines in error messages`() {
            val longIdentifier = "a".repeat(100)
            val source = "module $longIdentifier name"
            val lexer = Lexer(source)

            lexer.ptr = 7 + longIdentifier.length
            lexer.line = 0
            lexer.column = 7 + longIdentifier.length

            val exception = LexerException.InvalidIdentifier(longIdentifier)
            val result = exception.print(lexer).toString()

            assertTrue(result.contains(longIdentifier))
            assertTrue(result.contains("Неопознанный идентификатор: $longIdentifier"))
        }

        @Test
        fun `should handle positions beyond source length`() {
            val source = "test"
            val lexer = Lexer(source)

            lexer.ptr = 10 // Beyond source length
            lexer.line = 0
            lexer.column = 10

            val exception = LexerException.InvalidIdentifier("test")
            val result = exception.print(lexer).toString()

            // Should not throw exception when calculating line boundaries
            assertTrue(result.contains("Неопознанный идентификатор: test"))
        }

        @Test
        fun `should handle negative positions`() {
            val source = "test"
            val lexer = Lexer(source)

            lexer.ptr = -1
            lexer.line = 0
            lexer.column = -1

            val exception = LexerException.InvalidIdentifier("test")
            val result = exception.print(lexer).toString()

            // Should handle gracefully
            assertTrue(result.contains("Неопознанный идентификатор: test"))
        }

        @Test
        fun `should handle very large line numbers`() {
            val source = "test"
            val lexer = Lexer(source)

            lexer.ptr = 0
            lexer.line = 1000000
            lexer.column = 0

            val exception = LexerException.InvalidIdentifier("test")
            val result = exception.print(lexer).toString()

            // Should handle large numbers without issue
            assertTrue(result.contains("[1000001, 1]") || result.contains("Неопознанный идентификатор: test"))
        }
    }
}