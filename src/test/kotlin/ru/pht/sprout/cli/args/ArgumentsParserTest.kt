package ru.pht.sprout.cli.args

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class ArgumentsParserTest {
    @Test
    @DisplayName("Проверка коротких / длинных / раздельных команд")
    fun commandTest() {
        val parser = ArgumentsParser(arrayOf(
            Command.short("s", "short", emptyArray(), null),
            Command.long("long", emptyArray(), null),
            Command.long("sp li t", emptyArray(), null)
        ))
        assertEquals("short", parser.parse(Language.ENGLISH, arrayOf("-s")).definition.long)
        assertEquals("short", parser.parse(Language.ENGLISH, arrayOf("--short")).definition.long)
        assertEquals("long", parser.parse(Language.ENGLISH, arrayOf("--long")).definition.long)
        assertEquals("sp li t", parser.parse(Language.ENGLISH, arrayOf("--sp", "li", "t")).definition.long)
    }

    @ParameterizedTest
    @DisplayName("Проверка аргументов")
    @CsvSource(value = [
        "s, short, null, INT,           null, true,  null,     null    ",
        "s, short, null, INT,           null, false, 123,      null    ",
        "s, short, null, FLOAT,         null, false, 123.321,  null    ",
        "s, short, null, STRING,        null, false, 'text',   null    ",
        "s, short, null, STRING_VARARG, null, false, 'first',  'second'",
        "s, short, null, VARIATION,     v123, false, 'v123',   null    ",

        "null, long, null, INT,           null, true,  null,     null    ",
        "null, long, null, INT,           null, false, 123,      null    ",
        "null, long, null, FLOAT,         null, false, 123.321,  null    ",
        "null, long, null, STRING,        null, false, 'text',   null    ",
        "null, long, null, STRING_VARARG, null, false, 'first',  'second'",
        "null, long, null, VARIATION,     v123, false, 'v123',   null    ",

        "null, null, 'sp lt', INT,           null, true,  null,     null    ",
        "null, null, 'sp lt', INT,           null, false, 123,      null    ",
        "null, null, 'sp lt', FLOAT,         null, false, 123.321,  null    ",
        "null, null, 'sp lt', STRING,        null, false, 'text',   null    ",
        "null, null, 'sp lt', STRING_VARARG, null, false, 'first',  'second'",
        "null, null, 'sp lt', VARIATION,     v123, false, 'v123',   null    ",
    ], nullValues = ["null"])
    fun argumentsTest(
        short: String?,
        long: String?,
        split: String?,
        type: String,
        variants: String?,
        optional: Boolean,
        testValue: String?,
        testValue2: String?
    ) {
        val parser =
            CommandArgument.Definition(
                "value",
                null,
                CommandArgument.Type.valueOf(type),
                variants?.let { CommandArgument.Variants.of(listOf(it)) },
                optional
            ).let { argument ->
                ArgumentsParser(
                    arrayOf(
                        if (short != null)
                            Command.short(short, long!!, arrayOf(argument), null)
                        else if (long != null)
                            Command.long(long, arrayOf(argument), null)
                        else Command.long(split!!, arrayOf(argument), null),
                    )
                )
            }
        val args0 = if (short != null) arrayOf("-$short") else if (long != null) arrayOf("--$long") else split!!.split(' ').let { arrayOf("--${it[0]}", it[1]) }
        val args1 = if (testValue2 != null) arrayOf(testValue!!, testValue2) else if (testValue != null) arrayOf(testValue) else emptyArray()
        val args = args0 + args1
        val parsed = parser.parse(Language.ENGLISH, args)
        // Определение команды
        assertEquals(long ?: split, parsed.definition.long)
        // Проверка аргументов
        if (type == "STRING_VARARG") {
            assertEquals(
                listOf(testValue, testValue2),
                parsed.arguments.first().value
            )
        } else {
            if (testValue != null) {
                parsed.arguments.first().let {
                    assertEquals(testValue, it.value.toString())
                    assertEquals(type, it.definition.type.name)
                }
                if (testValue2 != null) {
                    assertEquals(testValue2, parsed.arguments[1].value.toString())
                    assertEquals(2, parsed.arguments.size)
                } else assertEquals(1, parsed.arguments.size)
            } else assertEquals(0, parsed.arguments.size)
        }
    }

    @Test
    @DisplayName("Проверка исключений")
    fun exceptionTest() {
        val parser = ArgumentsParser(arrayOf(
            Command.long(
                "number_fmt",
                arrayOf(
                    CommandArgument.Definition(
                        "number",
                        null,
                        CommandArgument.Type.INT,
                        null,
                        false
                    )
                ),
                null
            ),
            Command.long(
                "variant_find",
                arrayOf(
                    CommandArgument.Definition(
                        "variant",
                        null,
                        CommandArgument.Type.VARIATION,
                        CommandArgument.Variants.of(listOf("yes", "no")),
                        false
                    )
                ),
                null
            ),
            Command.long(
                "more_arguments",
                arrayOf(
                    CommandArgument.Definition(
                        "first",
                        null,
                        CommandArgument.Type.INT,
                        null,
                        false
                    ),
                    CommandArgument.Definition(
                        "second",
                        null,
                        CommandArgument.Type.FLOAT,
                        null,
                        false
                    ),
                    CommandArgument.Definition(
                        "third",
                        null,
                        CommandArgument.Type.STRING,
                        null,
                        false
                    )
                ),
                null
            ),
        ))

        assertEquals(
            "No command".fmt,
            assertThrows<ArgumentsParserException> {
                parser.parse(Language.ENGLISH, emptyArray())
            }.message
        )

        assertEquals(
            "Command '§sb-h§sr' not found".fmt,
            assertThrows<ArgumentsParserException> {
                parser.parse(Language.ENGLISH, arrayOf("-h"))
            }.message
        )
        assertEquals(
            "Command '§sb--help§sr' not found".fmt,
            assertThrows<ArgumentsParserException> {
                parser.parse(Language.ENGLISH, arrayOf("--help"))
            }.message
        )

        assertEquals(
            "'§sbhelp§sr' is not a command".fmt,
            assertThrows<ArgumentsParserException> {
                parser.parse(Language.ENGLISH, arrayOf("help"))
            }.message
        )

        assertEquals(
            "Argument formatting error '§sbnumber§sr'".fmt,
            assertThrows<ArgumentsParserException> {
                parser.parse(Language.ENGLISH, arrayOf("--number_fmt", "123.321"))
            }.message
        )
        assertEquals(
            "Unrecognized variant '§sbother§sr' of argument '§sbvariant§sr'".fmt,
            assertThrows<ArgumentsParserException> {
                parser.parse(Language.ENGLISH, arrayOf("--variant_find", "other"))
            }.message
        )

        assertEquals(
            "Missing required arguments: §sbfirst, second, third".fmt,
            assertThrows<ArgumentsParserException> {
                parser.parse(Language.ENGLISH, arrayOf("--more_arguments"))
            }.message
        )
    }
}