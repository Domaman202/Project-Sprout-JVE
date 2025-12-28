package ru.pht.sprout.utils

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#otherUtilsTest", disabledReason = "Тест выключен конфигурацией")
class JsonUtilsTest {
    @Test
    @DisplayName("Сериализация")
    fun serializeTest() {
        assertEquals(
            """
                {
                    "first": 12,
                    "second": 21
                }
            """.trimIndent(),
            JsonUtils.toJson(Pair(12, 21))
        )
    }

    @Test
    @DisplayName("Десериализация")
    fun deserializeTest() {
        assertEquals(
            Pair(202, 213),
            JsonUtils.fromJson("""
                {
                    "first": 202,
                    "second": 213
                }   
            """.trimIndent())
        )
    }
}