package ru.pht.sprout.utils.exception

import io.github.z4kn4fein.semver.constraints.Constraint
import kotlinx.io.IOException
import ru.DmN.translate.Language
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.DmN.translate.exception.ThrowableTranslator
import ru.pht.sprout.utils.SproutTranslate

class ModuleNotFoundException(val name: String, val version: Constraint) : IOException(), ITranslatedThrowable<ModuleNotFoundException> {
    override val translator: ThrowableTranslator<ModuleNotFoundException> get() = SproutTranslate.ExceptionTranslator
    override val message: String get() = this.translate(Language.ENGLISH)
}