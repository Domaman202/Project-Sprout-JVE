package ru.pht.sprout.cli.lang

import kotlinx.serialization.Serializable
import ru.pht.sprout.cli.App

@JvmInline
@Serializable
value class Translation(val key: String) {
    fun translate(lang: Language = App.LANG): String =
        lang.translate(this.key)

    companion object {
        inline fun <reified Caller> of(key: String): Translation =
            Translation("${Caller::class.java.name}.$key")

        inline fun <reified Caller> translate(lang: Language = App.LANG, key: String, vararg args: Pair<String, Any?>): String =
            lang.translate("${Caller::class.java.name}.$key", *args)
    }
}