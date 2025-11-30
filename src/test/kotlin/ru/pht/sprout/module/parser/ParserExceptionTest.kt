package ru.pht.sprout.module.parser

import org.junit.jupiter.api.assertThrows
import ru.pht.sprout.module.lexer.Lexer
import ru.pht.sprout.module.lexer.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserExceptionTest {
    @Test
    fun testUnsupported() {
        val parser = Parser(Lexer("(module \"my/custom/module\")"))
        val exception0 = assertThrows<ParserException.Wrapped> {
            parser.parse()
        }
        assertEquals(exception0.context.stage, "Парсинг заголовка")
        val exception1 = exception0.exception
        assertIs<ParserException.Unsupported>(exception1)
        val expected = """
            [1, 9] (module "my/custom/module")
                           ^~~~~~~~~~~~~~~~~~ Неподдерживаемый формат модуля
        """.trimIndent()
        assertEquals(expected, exception1.print(parser).toString())
    }

    @Test
    fun testValidation() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.X.0"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException.Wrapped> {
            parser.parse()
        }
        assertEquals(exception0.context.stage, "Парсинг заголовка 'pht/module'")
        val exception1 = exception0.exception
        assertIs<ParserException.Wrapped>(exception1)
        assertEquals(exception1.context.stage, "Парсинг аттрибутов модуля")
        val exception2 = exception1.exception
        assertIs<ParserException.ValidationException>(exception2)
        assertEquals(exception2.string, "1.X.0")
        val expected = """
            [3, 12]     {[vers "1.X.0"]})
                               ^~~~~~~ Невалидное значение '1.X.0'
        """.trimIndent()
        assertEquals(expected, exception2.print(parser).toString())
    }

    @Test
    fun testNotInitialized() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[vers "1.0.0"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException.Wrapped> {
            parser.parse()
        }
        assertEquals(exception0.context.stage, "Парсинг заголовка 'pht/module'")
        val exception1 = exception0.exception
        assertIs<ParserException.NotInitializedException>(exception1)
        assertEquals(exception1.field, "name")
        val expected = """
            [1, 1] (module "pht/module"
                   ^ Неинициализированное обязательное поле 'name'
        """.trimIndent()
        assertEquals(expected, exception1.print(parser).toString())
    }

    @Test
    fun testUnexpected() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example")
                {[vers "1.0.0"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException.Wrapped> {
            parser.parse()
        }
        assertEquals(exception0.context.stage, "Парсинг заголовка 'pht/module'")
        val exception1 = exception0.exception
        assertIs<ParserException.Wrapped>(exception1)
        assertEquals(exception1.context.stage, "Парсинг аттрибутов модуля")
        val exception2 = exception1.exception
        assertIs<ParserException.UnexpectedToken>(exception2)
        assertEquals(exception2.accepted.type, Token.Type.INSTR_END)
        val expected = """
            [2, 25]     {[name "pht/example")
                                            ^ Неожиданный токен INSTR_END
                                              Ожидались токены типа: 'ATTR_END'
        """.trimIndent()
        assertEquals(expected, exception2.print(parser).toString())
    }
}