package ru.pht.sprout.utils

import ru.pht.sprout.utils.lang.SproutTranslate
import ru.pht.sprout.utils.lang.TranslatedRuntimeException

/**
 * Ошибка неустановленного значения.
 */
class NotValueException : TranslatedRuntimeException(SproutTranslate.of<NotValueException>())