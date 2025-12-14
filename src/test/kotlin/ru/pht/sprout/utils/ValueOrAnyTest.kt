package ru.pht.sprout.utils

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import kotlin.test.*

@EnabledIf("ru.pht.sprout.TestConfigInternal#otherUtilsTest", disabledReason = "Тест выключен конфигурацией")
class ValueOrAnyTest {
    @Test
    @DisplayName("Проверка null значения")
    fun testNullValue() {
        val valueOrAny = ValueOrAny.of(null)
        assertFalse(valueOrAny.isAny)
        assertTrue(valueOrAny.isValue)
        assertNull(valueOrAny.value())
    }

    @Test
    @DisplayName("Проверка значения")
    fun testValue() {
        val valueOrAny = ValueOrAny.of("Test!")
        assertFalse(valueOrAny.isAny)
        assertTrue(valueOrAny.isValue)
        assertEquals("Test!", valueOrAny.value())
    }

    @Test
    @DisplayName("Проверка любого")
    fun testAny() {
        val valueOrAny = ValueOrAny.any<String>()
        assertTrue(valueOrAny.isAny)
        assertFalse(valueOrAny.isValue)
        assertEquals(
            assertThrows<NotValueException> {
                valueOrAny.value()
            }.translate(Language.ENGLISH),
            "Value has not been set".fmt
        )
    }

    @Test
    @DisplayName("Проверка хеша")
    fun hashTest() {
        assertEquals(ValueOrAny.any<Any?>().hashCode(), ValueOrAny.any<Any?>().hashCode())
        assertNotEquals(ValueOrAny.any<Any?>().hashCode(), ValueOrAny.of<Any?>(null).hashCode())
        assertEquals(ValueOrAny.of<Any?>(null).hashCode(), ValueOrAny.of<Any?>(null).hashCode())
        assertNotEquals(ValueOrAny.of<Any?>("Some").hashCode(), ValueOrAny.of<Any?>("Other").hashCode())
        assertEquals(ValueOrAny.of<Any?>("Some").hashCode(), ValueOrAny.of<Any?>("Some").hashCode())
    }
}