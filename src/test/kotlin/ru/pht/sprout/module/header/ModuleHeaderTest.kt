package ru.pht.sprout.module.header

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.pht.sprout.module.header.ModuleHeader.Companion.with
import ru.pht.sprout.utils.ValueOrAny

class ModuleHeaderTest {

    @Test
    fun testListWith() {
        val list1 = listOf("a", "b")
        val list2 = listOf("c", "d")
        val result = list1 with list2
        assertEquals(listOf("a", "b", "c", "d"), result)
    }

    @Test
    fun testNullListWith() {
        val list: List<String>? = null
        val list2 = listOf("c", "d")
        val result = list with list2
        assertEquals(listOf("c", "d"), result)
    }

    @Test
    fun testListOrAnyWith() {
        val list1 = ValueOrAny.of(listOf("a"))
        val list2 = ValueOrAny.of(listOf("b"))
        val result = list1 with list2
        assertTrue(result.isValue)
        assertEquals(listOf("a", "b"), result.value())
    }

    @Test
    fun testListOrAnyWithAny() {
        val list1 = ValueOrAny.of(listOf("a"))
        val any = ValueOrAny.any<List<String>>()
        val result = list1 with any
        assertTrue(result.isAny)
    }

    @Test
    fun testAnyWithListOrAny() {
        val any = ValueOrAny.any<List<String>>()
        val list2 = ValueOrAny.of(listOf("b"))
        val result = any with list2
        assertTrue(result.isAny)
    }

    @Test
    fun testNullListOrAnyWith() {
        val list: ValueOrAny<List<String>>? = null
        val list2 = ValueOrAny.of(listOf("b"))
        val result = list with list2
        assertTrue(result.isValue)
        assertEquals(listOf("b"), result.value())
    }
}
