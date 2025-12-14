package ru.pht.sprout.utils.lang

import kotlinx.io.IOException

/**
 * Запросчик файлов перевода.
 */
fun interface ITranslateResolver {
    /**
     * Запросить перевод.
     *
     * @param code Код языка.
     * @return `json` - перевод найден и прочитан, `null` - перевод не найден.
     * @throws IOException Ошибка во время чтения файла перевода.
     */
    @Throws(IOException::class)
    fun resolve(code: String): String?
}