package ru.pht.sprout.utils.exception

import ru.DmN.translate.Language
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator
import ru.pht.sprout.utils.SproutTranslate

/**
 * Ошибка неустановленного значения.
 */
class NotValueException : RuntimeException(), ITranslatedThrowable<NotValueException> {
    override val translator: ThrowableTranslator<NotValueException> get() = SproutTranslate.ExceptionTranslator
    override val message: String get() = this.translate(Language.ENGLISH)
}