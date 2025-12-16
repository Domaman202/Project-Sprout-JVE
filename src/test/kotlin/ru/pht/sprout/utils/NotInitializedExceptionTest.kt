package ru.pht.sprout.utils

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#otherUtilsTest", disabledReason = "Тест выключен конфигурацией")
class NotInitializedExceptionTest {
    @Test
    @DisplayName("Перевод")
    fun translateTest() {
        assertEquals(NotInitializedException("i").translate(Language.ENGLISH), "Uninitialized required field 'i'".fmt)
    }
}