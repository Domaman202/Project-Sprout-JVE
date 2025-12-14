package ru.pht.sprout.module.utils

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpUtilsTest {
    @Test
    fun clientWithoutLoggingTest() = runTest {
        assertEquals(HttpUtils.clientWithoutLogging().get("https://google.com").status, HttpStatusCode.OK)
    }
}