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
        val list1 = Module.ListOrAny.ofList(listOf("a"))
        val list2 = Module.ListOrAny.ofList(listOf("b"))
        val result = list1 with list2
        assertTrue(result.isList)
        assertEquals(listOf("a", "b"), result.list())
    }

    @Test
    fun testListOrAnyWithAny() {
        val list1 = Module.ListOrAny.ofList(listOf("a"))
        val any = Module.ListOrAny.any<String>()
        val result = list1 with any
        assertTrue(result.isAny)
    }

    @Test
    fun testAnyWithListOrAny() {
        val any = Module.ListOrAny.any<String>()
        val list2 = Module.ListOrAny.ofList(listOf("b"))
        val result = any with list2
        assertTrue(result.isAny)
    }

    @Test
    fun testNullListOrAnyWith() {
        val list: Module.ListOrAny<String>? = null
        val list2 = Module.ListOrAny.ofList(listOf("b"))
        val result = list with list2
        assertTrue(result.isList)
        assertEquals(listOf("b"), result.list())
    }
}
