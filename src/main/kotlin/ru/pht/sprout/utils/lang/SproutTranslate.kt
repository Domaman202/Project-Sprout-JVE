package ru.pht.sprout.utils.lang

import ru.pht.sprout.utils.NotInitializedException
import java.util.*

/**
 * Вспомогательный класс для загрузки перевода всех классов проекта "Росток".
 */
object SproutTranslate {
    init {
        Language.addResolver(NotInitializedException::class.java) { NotInitializedException::class.java.getResource("/sprout/lang/$it.json")?.readText(Charsets.UTF_8) }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun language(locale: Locale): Language =
        Language.of(locale)

    inline fun <reified Caller> of(): Translation =
        Translation.of<Caller>()

    inline fun <reified Caller> of(key: String): Translation =
        Translation.of<Caller>(key)

    inline fun <reified Caller> translate(lang: Language, key: String, vararg args: Pair<String, Any?>): String =
        Translation.translate<Caller>(lang, key, *args)
}