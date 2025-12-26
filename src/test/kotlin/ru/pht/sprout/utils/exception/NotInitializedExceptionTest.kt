package ru.pht.sprout.utils.exception

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#exceptionWithTranslateTest", disabledReason = "Тест выключен конфигурацией")
class NotInitializedExceptionTest {
    @Test
    @DisplayName("Перевод")
    fun translateTest() {
        val exception = NotInitializedException("i")
        assertEquals(
            "Uninitialized required field '§sbi§sr'".fmt,
            exception.message
        )
        assertEquals(
            "Uninitialized required field '§sbi§sr'".fmt,
            exception.translate(Language.ENGLISH)
        )
    }
}