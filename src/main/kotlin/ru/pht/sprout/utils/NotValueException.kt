package ru.pht.sprout.utils

import ru.DmN.translate.Language
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator

/**
 * Ошибка неустановленного значения.
 */
class NotValueException : RuntimeException(), ITranslatedThrowable<NotValueException> {
    override val translator: ThrowableTranslator<NotValueException>
        get() = SproutTranslate.ExceptionTranslator
    override val message: String
        get() = this.translate(Language.ENGLISH)
}