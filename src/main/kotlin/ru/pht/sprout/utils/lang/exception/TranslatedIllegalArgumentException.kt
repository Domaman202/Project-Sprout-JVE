package ru.pht.sprout.utils.lang.exception

import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.Translation

open class TranslatedIllegalArgumentException : IllegalArgumentException, ITranslatedException {
    private val args: Array<out Pair<String, Any?>>
    private val translation0: Translation?
    override val translation: Translation?
        get() = this.translation0

    constructor(
        translation: Translation?,
        vararg args: Pair<String, Any?>
    ) : super(translation?.translate(Language.ENGLISH, *args)) {
        this.translation0 = translation
        this.args = args
    }

    constructor(
        message: String?,
        translation: Translation?,
        vararg args: Pair<String, Any?>
    ) : super(message) {
        this.translation0 = translation
        this.args = args
    }

    constructor(
        message: String?,
        cause: Throwable?,
        translation: Translation?,
        vararg args: Pair<String, Any?>
    ) : super(message, cause) {
        this.translation0 = translation
        this.args = args
    }

    constructor(
        cause: Throwable?,
        translation: Translation?,
        vararg args: Pair<String, Any?>
    ) : super(cause) {
        this.translation0 = translation
        this.args = args
    }

    override fun translate(language: Language): String? =
        this.translation?.translate(language, *this.args)
}