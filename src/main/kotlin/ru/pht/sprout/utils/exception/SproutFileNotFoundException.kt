package ru.pht.sprout.utils.exception

import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import java.io.FileNotFoundException

class SproutFileNotFoundException(message: TranslationKey, vararg args: Pair<String, Any?>) : FileNotFoundException(), ISproutException<SproutFileNotFoundException> {
    override val translatingMessage: TranslationKey = message
    override val translatingMessageArguments: Array<out Pair<String, Any?>> = args
    override val message: String get() = this.translate(Language.ENGLISH)
}