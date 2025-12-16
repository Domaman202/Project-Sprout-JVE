package ru.pht.sprout.utils.lang.exception

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.Translation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@EnabledIf("ru.pht.sprout.TestConfigInternal#translateTest", disabledReason = "Тест выключен конфигурацией")
class ITranslatedExceptionTest {
    @Test
    @DisplayName("Метод translate по умолчанию")
    fun defaultTranslateTest() {
        val normal: ITranslatedException = object : ITranslatedException {
            override val translation = Translation.of<ITranslatedExceptionTest>("defaultTranslateTest")
        }
        val nullable: ITranslatedException = object : ITranslatedException {
            override val translation = null
        }
        assertEquals(normal.translate(Language.ENGLISH), "Default Translate Method".fmt)
        assertNull(nullable.translate(Language.ENGLISH))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            Language.addResolver(ITranslatedExceptionTest::class.java) { ITranslatedExceptionTest::class.java.getResource("/sprout/test/lang/$it.json")?.readText(Charsets.UTF_8) }
        }
    }
}