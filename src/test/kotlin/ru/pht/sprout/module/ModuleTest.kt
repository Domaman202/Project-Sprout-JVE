package ru.pht.sprout.module

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.pht.sprout.module.Module.Companion.with

class ModuleTest {

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
        val list1 = Module.ValueOrAny.of(listOf("a"))
        val list2 = Module.ValueOrAny.of(listOf("b"))
        val result = list1 with list2
        assertTrue(result.isValue)
        assertEquals(listOf("a", "b"), result.value())
    }

    @Test
    fun testListOrAnyWithAny() {
        val list1 = Module.ValueOrAny.of(listOf("a"))
        val any = Module.ValueOrAny.any<List<String>>()
        val result = list1 with any
        assertTrue(result.isAny)
    }

    @Test
    fun testAnyWithListOrAny() {
        val any = Module.ValueOrAny.any<List<String>>()
        val list2 = Module.ValueOrAny.of(listOf("b"))
        val result = any with list2
        assertTrue(result.isAny)
    }

    @Test
    fun testNullListOrAnyWith() {
        val list: Module.ValueOrAny<List<String>>? = null
        val list2 = Module.ValueOrAny.of(listOf("b"))
        val result = list with list2
        assertTrue(result.isValue)
        assertEquals(listOf("b"), result.value())
    }
}
