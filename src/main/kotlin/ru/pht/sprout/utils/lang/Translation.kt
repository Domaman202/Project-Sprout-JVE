package ru.pht.sprout.utils.lang

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Translation(val key: String) {
    fun translate(lang: Language): String =
        lang.translate(this.key)
    fun translate(lang: Language, vararg args: Pair<String, Any?>): String =
        lang.translate(this.key, *args)

    companion object {
        inline fun <reified Caller> of(): Translation =
            Translation("${Caller::class.java.name}")

        inline fun <reified Caller> of(key: String): Translation =
            Translation("${Caller::class.java.name}.$key")

        inline fun <reified Caller> translate(lang: Language, key: String, vararg args: Pair<String, Any?>): String =
            lang.translate("${Caller::class.java.name}.$key", *args)
    }
}