package ru.pht.sprout.module.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*

/**
 * Утилиты для работы с сетевым соединением.
 */
object HttpUtils {
    /**
     * [io.ktor.client.HttpClient] с выключенным логированием.
     */
    fun clientWithoutLogging(): HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.NONE
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 2_500
        }

        install(UserAgent) {
            agent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
    }
}