package ru.pht.sprout.utils

import ru.DmN.translate.Language
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator

/**
 * Ошибка инициализации обязательного поля.
 *
 * @param field Имя поля.
 */
class NotInitializedException(val field: String) : RuntimeException(), ITranslatedThrowable<NotInitializedException> {
    override val translator: ThrowableTranslator<NotInitializedException>
        get() = SproutTranslate.ExceptionTranslator
    override val message: String
        get() = this.translate(Language.ENGLISH)
}