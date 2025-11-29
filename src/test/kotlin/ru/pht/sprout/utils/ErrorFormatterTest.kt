package ru.pht.sprout.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class ErrorFormatterTest {

    @Test
    fun testFormatErrorWithToken() {
        val source = "line1\nline2\nline3"
        val expected = "[2, 3] line2\n         ^~ Some error message"
        val actual = ErrorFormatter.formatErrorWithToken(source, 8, 2, 1, 2, "Some error message")
        assertEquals(expected, actual)
    }
}
