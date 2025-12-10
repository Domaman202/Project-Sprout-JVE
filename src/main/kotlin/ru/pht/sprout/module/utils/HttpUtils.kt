package ru.pht.sprout.module.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*

object HttpUtils {
    fun clientWithoutLogging(): HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.NONE
        }
    }
}