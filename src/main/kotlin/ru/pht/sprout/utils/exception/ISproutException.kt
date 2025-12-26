package ru.pht.sprout.utils.exception

import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator
import ru.pht.sprout.utils.SproutTranslate

interface ISproutException<T> : ITranslatedThrowable<T> where T : Throwable, T : ITranslatedThrowable<T>, T : ISproutException<T> {
    val translatingMessage: TranslationKey
    val translatingMessageArguments: Array<out Pair<String, Any?>>

    override val translator: ThrowableTranslator<T>
        get() = translator()

    companion object {
        fun <T> translator(): ThrowableTranslator<T> where T : Throwable, T : ITranslatedThrowable<T>, T : ISproutException<T>  =
            SproutExceptionTranslator
    }

    private object SproutExceptionTranslator : ThrowableTranslator<Throwable>() {
        override fun translate(language: Language, throwable: Throwable): String {
            throwable as ISproutException<*>
            return SproutTranslate.translate(language, throwable.translatingMessage, *throwable.translatingMessageArguments)
        }
    }
}