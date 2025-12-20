package ru.pht.sprout.utils

import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator

class SproutIllegalArgumentException : IllegalArgumentException, ITranslatedThrowable<SproutIllegalArgumentException> {
    private val translatingMessage: TranslationKey
    private val translatingMessageArguments: Array<out Pair<String, Any?>>

    override val translator: ThrowableTranslator<SproutIllegalArgumentException>
        get() = Translator
    override val message: String
        get() = this.translate(Language.ENGLISH)

    constructor(message: TranslationKey, vararg args: Pair<String, Any?>) : super() {
        this.translatingMessage = message
        this.translatingMessageArguments = args
    }

    private object Translator : ThrowableTranslator<SproutIllegalArgumentException>() {
        override fun translate(language: Language, throwable: SproutIllegalArgumentException): String =
            SproutTranslate.translate(Language.ENGLISH, throwable.translatingMessage, *throwable.translatingMessageArguments)
    }
}