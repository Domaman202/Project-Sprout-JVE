package ru.pht.sprout.utils.exception

import ru.DmN.translate.Language
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator
import ru.pht.sprout.utils.SproutTranslate

/**
 * Ошибка инициализации обязательного поля.
 *
 * @param field Имя поля.
 */
class NotInitializedException(val field: String) : RuntimeException(), ITranslatedThrowable<NotInitializedException> {
    override val translator: ThrowableTranslator<NotInitializedException> get() = SproutTranslate.ExceptionTranslator
    override val message: String get() = this.translate(Language.ENGLISH)
}