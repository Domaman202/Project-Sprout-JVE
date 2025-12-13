package ru.pht.sprout.utils.lang

/**
 * Исключение, которое содержит переводимое сообщение.
 */
interface ITranslatedException {
    /**
     * Ключ перевода сообщения.
     */
    val translation: Translation

    /**
     * Перевод сообщения.
     *
     * @param language Язык.
     * @return Переведённое сообщение.
     */
    fun translate(language: Language): String =
        translation.translate(language)
}