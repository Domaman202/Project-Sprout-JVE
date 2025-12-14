package ru.pht.sprout.utils

import org.junit.jupiter.api.DisplayName
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class NotInitializedExceptionTest {
    @Test
    @DisplayName("Проверка перевода")
    fun translateTest() {
        assertEquals(NotInitializedException("i").translate(Language.ENGLISH), "Uninitialized required field 'i'".fmt)
    }
}