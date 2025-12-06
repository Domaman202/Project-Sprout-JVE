package ru.pht.sprout.utils

/**
 * Вариация `значение` или `любое`.
 */
class ValueOrAny<T> private constructor(private val value: T?, private val any: Boolean) {
    /// Это значение?
    val isValue: Boolean
        get() = !this.any
    /// Это любое?
    val isAny: Boolean
        get() = this.any

    /**
     * Возвращает значения, иначе кидает исключение.
     *
     * @return Значение.
     * @throws NotValueException Если значения не было установлено.
     */
    @Suppress("UNCHECKED_CAST")
    fun value(): T {
        if (this.isAny)
            throw NotValueException()
        return this.value as T
    }

    companion object {
        private val ANY = ValueOrAny(null, true)

        /**
         * @param value Значение.
         * @return Значение.
         */
        fun <T> of(value: T): ValueOrAny<T> =
            ValueOrAny(value, false)

        /**
         * @return Любое.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> any(): ValueOrAny<T> =
            ANY as ValueOrAny<T>
    }
}