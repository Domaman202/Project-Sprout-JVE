package ru.pht.sprout.utils.lang

open class TranslatedRuntimeException : RuntimeException, ITranslatedException {
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

    protected constructor(
        message: String?,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean,
        translation: Translation?,
        vararg args: Pair<String, Any?>
    ) : super(message, cause, enableSuppression, writableStackTrace) {
        this.translation0 = translation
        this.args = args
    }

    override fun translate(language: Language): String? =
        this.translation?.translate(language, *this.args)
}