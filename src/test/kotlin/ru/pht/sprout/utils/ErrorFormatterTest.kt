package ru.pht.sprout.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class ErrorFormatterTest {

    @Test
    fun testFormatErrorWithToken() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val expected = """
            [2, 1] line2
                   ^~~~~ Some error message
        """.trimIndent()
        val actual = ErrorFormatter.formatErrorWithToken(source, 6, 5, 1, 0, "Some error message")
        assertEquals(expected, actual)
    }
}
