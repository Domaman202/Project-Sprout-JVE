package ru.pht.sprout.module.lexer

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LexerExceptionTest {
    @Test
    fun testUncompletedString() {
        val lexer = Lexer("\"hello")
        val exception = assertThrows(LexerException.UncompletedString::class.java) {
            while (lexer.hasNext()) {
                lexer.next()
            }
        }
        val expected = """
            [1, 1] "hello
                   ^ Незавершённая строка
        """.trimIndent()
        assertEquals(expected, exception.print(lexer).toString())
    }

    @Test
    fun testInvalidIdentifier() {
        val lexer = Lexer("{[invalid-identifier allow}]")
        val exception = assertThrows(LexerException.InvalidIdentifier::class.java) {
            while (lexer.hasNext()) {
                lexer.next()
            }
        }
        assertEquals(exception.identifier, "invalid-identifier")
        val expected = """
            [1, 3] {[invalid-identifier allow}]
                     ^~~~~~~~~~~~~~~~~~ Неопознанный идентификатор
        """.trimIndent()
        assertEquals(expected, exception.print(lexer).toString())
    }

    @Test
    fun testUnexpectedSymbol() {
        val lexer = Lexer("(module\n %)")
        val exception = assertThrows(LexerException.UnexpectedSymbol::class.java) {
            while (lexer.hasNext()) {
                lexer.next()
            }
        }
        assertEquals(exception.symbol, '%')
        val expected = """
            [2, 2]  %)
                    ^ Неожиданный символ
        """.trimIndent()
        assertEquals(expected, exception.print(lexer).toString())
    }
}