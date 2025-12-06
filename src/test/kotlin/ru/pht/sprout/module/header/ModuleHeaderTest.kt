package ru.pht.sprout.module.header

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.pht.sprout.module.header.ModuleHeader.Companion.with
import ru.pht.sprout.module.header.ModuleHeader.Dependency
import ru.pht.sprout.module.header.ModuleHeader.PathOrDependency
import ru.pht.sprout.utils.NotValueException
import ru.pht.sprout.utils.ValueOrAny
import kotlin.test.assertFalse

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

    @Nested
    inner class PathOrDependencyTest {
        @Test
        fun testPathSuccess() {
            val pathOrDependency = PathOrDependency.ofPath("path")
            assertTrue(pathOrDependency.isPath)
            assertFalse(pathOrDependency.isDependency)
            assertEquals("path", pathOrDependency.path())
        }

        @Test
        fun testPathFail() {
            val pathOrDependency = PathOrDependency.ofDependency(Dependency.Builder().name("dependency").build())
            assertFalse(pathOrDependency.isPath)
            assertTrue(pathOrDependency.isDependency)
            assertThrows<NotValueException> { pathOrDependency.path() }
        }

        @Test
        fun testDependencySuccess() {
            val pathOrDependency = PathOrDependency.ofDependency(Dependency.Builder().name("dependency").build())
            assertFalse(pathOrDependency.isPath)
            assertTrue(pathOrDependency.isDependency)
            assertEquals("dependency", pathOrDependency.dependency().name)
        }

        @Test
        fun testDependencyFail() {
            val pathOrDependency = PathOrDependency.ofPath("path")
            assertTrue(pathOrDependency.isPath)
            assertFalse(pathOrDependency.isDependency)
            assertThrows<NotValueException> { pathOrDependency.dependency() }
        }
    }
}
