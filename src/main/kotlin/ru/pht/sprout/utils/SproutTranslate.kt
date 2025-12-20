package ru.pht.sprout.utils

import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import ru.DmN.translate.exception.ReflectiveThrowableTranslator
import ru.DmN.translate.provider.ResourceTranslationProvider

/**
 * Поставщик переводов в пределах проекта "Росток".
 */
object SproutTranslate : ResourceTranslationProvider("sprout/lang") {
    inline fun <reified T> of(language: Language, vararg args: Pair<String, Any?>): String =
        this.translate(language, TranslationKey(T::class.java.name), *args)
    inline fun <reified T> of(language: Language, category: String, vararg args: Pair<String, Any?>): String =
        this.translate(language, TranslationKey("${T::class.java.name}.$category"), *args)

    object ExceptionTranslator : ReflectiveThrowableTranslator<Throwable>(this)
}
