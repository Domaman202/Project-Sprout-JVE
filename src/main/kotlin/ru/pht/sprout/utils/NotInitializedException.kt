package ru.pht.sprout.utils

/**
 * Ошибка инициализации обязательного поля.
 *
 * @param field Имя поля.
 */
class NotInitializedException(val field: String) : RuntimeException("Неинициализированное обязательное поле '$field'")