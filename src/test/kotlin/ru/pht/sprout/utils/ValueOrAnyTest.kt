package ru.pht.sprout.utils

import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class ValueOrAnyTest {
    @Test
    fun testNullValue() {
        val valueOrAny = ValueOrAny.of(null)
        assertFalse(valueOrAny.isAny)
        assertTrue(valueOrAny.isValue)
        assertNull(valueOrAny.value())
    }

    @Test
    fun testValue() {
        val valueOrAny = ValueOrAny.of("Test!")
        assertFalse(valueOrAny.isAny)
        assertTrue(valueOrAny.isValue)
        assertEquals("Test!", valueOrAny.value())
    }

    @Test
    fun testAny() {
        val valueOrAny = ValueOrAny.any<String>()
        assertTrue(valueOrAny.isAny)
        assertFalse(valueOrAny.isValue)
        assertThrows<NotValueException> { valueOrAny.value() }
    }
}