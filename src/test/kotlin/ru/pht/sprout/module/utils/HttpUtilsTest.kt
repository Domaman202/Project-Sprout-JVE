package ru.pht.sprout.module.utils

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.condition.EnabledIf
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#realNetTest", disabledReason = "Тест выключен конфигурацией")
class HttpUtilsTest {
    @Test
    fun clientWithoutLoggingTest() = runTest {
        assertEquals(HttpUtils.clientWithoutLogging().get("https://google.com").status, HttpStatusCode.OK)
    }
}