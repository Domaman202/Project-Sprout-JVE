package ru.pht.sprout.utils.fmt

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.exception.TranslatedRuntimeException
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#fmtTest", disabledReason = "Тест выключен конфигурацией")
class FmtUtilsTest {
    @Test
    @DisplayName("Проверка форматирования")
    fun testValid() {
        assertEquals(
            """
                §§
                §f0black
                §f1red
                §f2green
                §f3yellow
                §f4blue
                §f5magenta
                §f6cyan
                §f7white
                §f8bright_black
                §f9bright_red
                §fabright_green
                §fbbright_yellow
                §fcbright_blue
                §fdbright_magenta
                §febright_cyan
                §ffbright_white
                §b0black
                §b1red
                §b2green
                §b3yellow
                §b4blue
                §b5magenta
                §b6cyan
                §b7white
                §b8bright_black
                §b9bright_red
                §babright_green
                §bbbright_yellow
                §bcbright_blue
                §bdbright_magenta
                §bebright_cyan
                §bfbright_white
                §srreset
                §sbbold
                §siitalic
                §suunderline
                reset
            """.trimIndent().fmt,
            """
                §§
                [30mblack
                [31mred
                [32mgreen
                [33myellow
                [34mblue
                [35mmagenta
                [36mcyan
                [37mwhite
                [90mbright_black
                [91mbright_red
                [92mbright_green
                [93mbright_yellow
                [94mbright_blue
                [95mbright_magenta
                [96mbright_cyan
                [97mbright_white
                [40mblack
                [41mred
                [42mgreen
                [43myellow
                [44mblue
                [45mmagenta
                [46mcyan
                [47mwhite
                [100mbright_black
                [101mbright_red
                [102mbright_green
                [103mbright_yellow
                [104mbright_blue
                [105mbright_magenta
                [106mbright_cyan
                [107mbright_white
                [00mreset
                [01mbold
                [03mitalic
                [04munderline
                reset[00m
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Ошибки и их перевод")
    fun testException() {
        assertEquals(
            assertThrows<TranslatedRuntimeException> {
                "§fx".fmt
            }.translate(Language.ENGLISH),
            "Unexpected color code of foreground 'x'".fmt
        )
        assertEquals(
            assertThrows<TranslatedRuntimeException> {
                "§bx".fmt
            }.translate(Language.ENGLISH),
            "Unexpected color code of background 'x'".fmt
        )
        assertEquals(
            assertThrows<TranslatedRuntimeException> {
                "§sx".fmt
            }.translate(Language.ENGLISH),
            "Unexpected style code 'x'".fmt
        )
        assertEquals(
            assertThrows<TranslatedRuntimeException> {
                "§x".fmt
            }.translate(Language.ENGLISH),
            "Unexpected formatting code 'x'".fmt
        )
    }
}