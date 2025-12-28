package ru.pht.sprout.utils

import kotlinx.serialization.json.Json

/**
 * Утилиты для работы с JSON.
 */
object JsonUtils {
    /**
     * Сконфиругированный JSON сериализатор / десериализатор.
     */
    val JSON = Json { prettyPrint = true }

    /**
     * Сериализация в json.
     *
     * @param T Тип сериализуемого объекта.
     * @param value Сериализуемый объект типа.
     * @return JSON.
     */
    inline fun <reified T> toJson(value: T): String {
        return JSON.encodeToString<T>(value)
    }

    /**
     * Десериализация из [json].
     *
     * @param T Тип десериализуемого объекта.
     * @param json JSON.
     * @return Десериализованный объект.
     */
    inline fun <reified T> fromJson(json: String): T {
        return JSON.decodeFromString(json)
    }
}