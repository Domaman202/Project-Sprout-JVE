package ru.pht.sprout.utils.exception

import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey

class SproutIllegalStateException(message: TranslationKey, vararg args: Pair<String, Any?>) : IllegalStateException(), ISproutException<SproutFileNotFoundException> {
    override val translatingMessage: TranslationKey = message
    override val translatingMessageArguments: Array<out Pair<String, Any?>> = args
    override val message: String get() = this.translate(Language.ENGLISH)
}