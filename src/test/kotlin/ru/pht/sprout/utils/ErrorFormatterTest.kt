package ru.pht.sprout.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ErrorFormatterTest {

    @Test
    fun `formatError should format single-line error correctly`() {
        val source = "let x = 5"
        val result = ErrorFormatter.formatError(source, 4, 1, "Unexpected character")

        val expected = """
            [1, 5] let x = 5
                       ^ Unexpected character
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should handle multi-line source`() {
        val source = """
            let x = 5
            let y = 
        """.trimIndent()

        val result = ErrorFormatter.formatError(source, 20, 1, "Expected expression")

        val expected = """
            [2, 9] let y = 
                           ^ Expected expression
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should handle tabs correctly`() {
        val source = "\t\tvar x"
        val result = ErrorFormatter.formatError(source, 2, 3, "Invalid keyword")

        val expected = """
            [1, 3]         var x
                           ^~~ Invalid keyword
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should handle zero-length error`() {
        val source = "return"
        val result = ErrorFormatter.formatError(source, 6, 0, "Missing semicolon")

        val expected = """
            [1, 7] return
                         ^ Missing semicolon
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should clamp out-of-bounds position`() {
        val source = "a"
        val result = ErrorFormatter.formatError(source, 5, 1, "Position out of bounds")

        val expected = """
            [1, 2] a
                    ^ Position out of bounds
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should handle negative length`() {
        val source = "abc"
        val result = ErrorFormatter.formatError(source, 1, -1, "Negative length")

        val expected = """
            [1, 2] abc
                    ^ Negative length
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should format multi-line message`() {
        val source = "foo bar"
        val result = ErrorFormatter.formatError(
            source,
            4,
            3,
            "Unknown variable\nDid you mean 'foo'?\n  Suggested fix"
        )

        val expected = """
            [1, 5] foo bar
                       ^~~ Unknown variable
                           Did you mean 'foo'?
                             Suggested fix
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should handle empty source`() {
        val source = ""
        val result = ErrorFormatter.formatError(source, 0, 1, "Empty source")

        val expected = """
            [1, 1] 
                   ^ Empty source
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatErrorWithToken should format using explicit coordinates`() {
        val source = "func main()\n  x = 5"
        val result = ErrorFormatter.formatErrorWithToken(
            source,
            tokenStart = 13,
            tokenLength = 1,
            line = 1,
            column = 2,
            message = "Undeclared variable"
        )

        val expected = """
            [2, 3]   x = 5
                    ^ Undeclared variable
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatErrorWithToken should handle tab characters`() {
        val source = "\titem\tvalue"
        val result = ErrorFormatter.formatErrorWithToken(
            source,
            tokenStart = 5,
            tokenLength = 5,
            line = 0,
            column = 5,
            message = "Invalid value"
        )

        val expected = """
            [1, 6]     item    value
                           ^~~~~~~ Invalid value
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatErrorWithToken should clamp invalid token position`() {
        val source = "test"
        val result = ErrorFormatter.formatErrorWithToken(
            source,
            tokenStart = 10,
            tokenLength = 2,
            line = 0,
            column = 10,
            message = "Invalid token"
        )

        val expected = """
            [1, 11] test
                        ^ Invalid token
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should handle error at line start`() {
        val source = "first\nsecond\nthird"
        val result = ErrorFormatter.formatError(source, 6, 6, "Invalid statement")

        val expected = """
            [2, 1] second
                   ^~~~~~ Invalid statement
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatError should handle error at line end`() {
        val source = "begin end"
        val result = ErrorFormatter.formatError(source, 5, 3, "Unexpected whitespace")

        val expected = """
            [1, 6] begin end
                        ^~~ Unexpected whitespace
        """.trimIndent()

        assertEquals(expected, result)
    }
}