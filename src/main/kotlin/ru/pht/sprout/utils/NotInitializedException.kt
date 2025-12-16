package ru.pht.sprout.utils

import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.SproutTranslate
import ru.pht.sprout.utils.lang.exception.TranslatedRuntimeException

/**
 * Ошибка инициализации обязательного поля.
 *
 * @param field Имя поля.
 */
class NotInitializedException(val field: String) : TranslatedRuntimeException(SproutTranslate.of<NotInitializedException>()) {
    override fun translate(language: Language): String? =
        this.translation?.translate(language, Pair("field", this.field))
}