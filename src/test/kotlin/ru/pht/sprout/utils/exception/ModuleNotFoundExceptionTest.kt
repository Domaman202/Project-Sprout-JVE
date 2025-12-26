package ru.pht.sprout.utils.exception

import io.github.z4kn4fein.semver.constraints.toConstraint
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#exceptionWithTranslateTest", disabledReason = "Тест выключен конфигурацией")
class ModuleNotFoundExceptionTest {
    @Test
    @DisplayName("Перевод")
    fun translateTest() {
        val exception = ModuleNotFoundException("example/module", "5.8.8".toConstraint())
        assertEquals(
            "Module '§sbexample/module§sr' version '§sb=5.8.8§sr' not found".fmt,
            exception.message
        )
        assertEquals(
            "Module '§sbexample/module§sr' version '§sb=5.8.8§sr' not found".fmt,
            exception.translate(Language.ENGLISH)
        )
    }
}