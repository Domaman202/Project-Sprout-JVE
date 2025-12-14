package ru.pht.sprout.module.header.lexer

import io.github.z4kn4fein.semver.toVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.pht.sprout.module.header.ModuleHeader.Dependency
import ru.pht.sprout.module.header.ModuleHeader.PathOrDependency
import ru.pht.sprout.module.header.parser.Parser
import ru.pht.sprout.module.header.parser.ParserException
import ru.pht.sprout.utils.ValueOrAny
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserTest {
    @Test
    @DisplayName("Парсинг заголовка из документации")
    fun exampleHeaderParseTest() {
        val header = Parser(Lexer("""
            (module "pht/module"
                ; Имя и версия модуля
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                
                ; Описание и авторы
                {[desc "Пример модуля Пихты"]}
                {[auth ["DomamaN202" "Phantom"]]}
                
                ; Зависимости (автоматически)
                {[deps ["pht/core"]]}
                {[uses ["pht/core"]]}
                
                ; Зависимости (продвинутая работа)
                {[deps [
                    (module
                        ; Имя модуля
                        {[name "pht/math"]}
                        ; Версия модуля [по умолчанию]
                        {[vers [*]]}
                        ; Включаем использование
                        {[uses allow]}
                        ; Выключаем все адаптеры
                        {[adapters []]}
                        ; Включаем фичу fast-math
                        {[features ["fast-math"]]})
                    (module
                        ; Имя модуля
                        {[name "pht/util"]}
                        ; Версия модуля
                        {[vers "1.0.0"]}
                        ; Выключаем использование [по умолчанию]
                        {[uses deny]}
                        ; Включаем все адаптеры [по умолчанию]
                        {[adapters [*]]}
                        ; Выключаем все фичи
                        {[no-features [*]]})]]}
                
                ; Инъекции
                ; - Разрешаем инъекции из модуля
                {[inject-into-chain allow]}
                ; - Разрешаем инъекции в модули
                {[inject-into-module ["pht/core"]]}
                ; - Разрешаем инъекции в модуль [по умолчанию]
                {[no-inject-from-chain deny]}
                ; - Запрещаем инъекции из модулей
                {[no-inject-from-module ["pht/core"]]}
                
                ; Импорт и экспорт [по умолчанию]
                {[imports [*]]}
                {[exports [*]]}
                
                ; Импорт и экспорт (конкретно)
                ; - Импортирует адаптеры, типы и функции
                {[imports [adapters types functions]]}
                ; - Экспортирует плагины и макросы
                {[exports [plugins macros]]}
                
                ; Доступные фичи
                {[features [
                    ; Фича по умолчанию
                    ["log"  default]
                    ; Фича опциональная
                    ["fast" optional]]]}
                
                ; Исходный код, ресурсы и код плагинов (директории) [по умолчанию]
                {[src ["src/*"]]}
                {[res ["res/*"]]}
                {[plg ["plg/*"]]}
                
                ; Исходный код, ресурсы и код плагинов (файлы по расширения)
                {[src ["src/*.pht"]]}
                {[res ["res/*.png"]]}
                {[plg ["plg/*.pht"]]}
                
                ; Исходный код, ресурсы и код плагинов (конкретные файлы)
                {[src ["src/main.pht"]]}
                {[res ["res/icon.png"]]}
                {[plg ["plg/main.pht"]]}
                
                ; Исходный код, ресурсы и код плагинов (собираемые из стороннего модуля)
                {[src [(module {[name "pht/example/sources"]})]]}
                {[res [(module {[name "pht/example/resources"]})]]}
                {[plg [(module {[name "pht/example/plugin"]})]]})
        """.trimIndent())).parse()
        assertNotNull(header)
        assertEquals(header.name, "pht/example")
        assertEquals(header.version, "1.0.0".toVersion())
        assertEquals(header.description, "Пример модуля Пихты")
        assertEquals(header.authors, listOf("DomamaN202", "Phantom"))
        assertTrue(header.imports.isAny)
        assertTrue(header.exports.isAny)
        assertEquals(header.features, listOf(Pair("log", true), Pair("fast", false)))
        assertEquals(
            header.source,
            listOf(
                PathOrDependency.ofPath("src/*"),
                PathOrDependency.ofPath("src/*.pht"),
                PathOrDependency.ofPath("src/main.pht"),
                PathOrDependency.ofDependency(Dependency.Builder().name("pht/example/sources").version(ValueOrAny.any()).build())
            )
        )
        assertEquals(
            header.resource,
            listOf(
                PathOrDependency.ofPath("res/*"),
                PathOrDependency.ofPath("res/*.png"),
                PathOrDependency.ofPath("res/icon.png"),
                PathOrDependency.ofDependency(Dependency.Builder().name("pht/example/resources").version(ValueOrAny.any()).build())
            )
        )
        assertEquals(
            header.plugin,
            listOf(
                PathOrDependency.ofPath("plg/*"),
                PathOrDependency.ofPath("plg/*.pht"),
                PathOrDependency.ofPath("plg/main.pht"),
                PathOrDependency.ofDependency(Dependency.Builder().name("pht/example/plugin").version(ValueOrAny.any()).build())
            )
        )
    }

    @Test
    @DisplayName("Перехват ошибки лексера")
    fun lexerThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<LexerException.UncompletedString>(exception0)
        assertEquals(exception1.message, "Uncompleted string".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [3, 12]     {[vers "1.0.0]})
                                   ^ Uncompleted string
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка неподдерживаемого формата модуля")
    fun unsupportedHeaderThrowTest() {
        val parser = Parser(Lexer("""
            (module "my/header"
                {[name "pht/example"]}
                {[vers "1.0.0"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.UnsupportedHeader>(exception0)
        assertEquals(exception1.format, "my/header")
        assertEquals(exception1.message, "Unsupported module format '§sbmy/header§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "my/header"
                       ^ 
                [1, 9] (module "my/header"
                               ^~~~~~~~~~~ Unsupported module format '§sbmy/header§sr'
            """.trimIndent().fmt
        )
    }

    @ParameterizedTest
    @DisplayName("Ошибка валидации имени")
    @CsvSource(
        "' '",
        "-abc", "/abc", "abc-", "abc/",
        "@bc", "a c", "a.c", "a+c",
        "a--d", "a//d", "a-/d", "a/-d",
        "---", "///", "-/", "/-"
    )
    fun nameValidationThrowTest(name: String) {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "$name"]}
                {[vers "1.0.0"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, name)
        assertEquals(exception1.message, "Invalid value '§sb$name§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [2, 5]     {[name "$name"]}
                           ^~ 
                [2, 12]     {[name "$name"]}
                                   ^${"~".repeat(name.length)}~ Invalid value '§sb$name§sr'
            """.trimIndent().fmt
        )
    }

    @ParameterizedTest
    @DisplayName("Ошибка валидации версии")
    @CsvSource(
        "1", "1.2", "1.2.3.4",
        "a.2.3", "1.b.3", "1.2.c", "1.2.3?",
        "01.2.3", "1.02.3", "1.2.03",
        "1.2.3-", "1.2.3-alpha.", "1.2.3-alpha..beta", "1.2.3-.beta",
        "1.2.3-alpha@beta", "1.2.3-alpha beta",
        "1.2.3-01", "1.2.3-1.02.alpha", "1.2.3-alpha.001",
        "1.2.3+", "1.2.3+alpha.", "1.2.3+alpha..beta", "1.2.3+.beta", "1.2.3-alpha+",
        "1.2.3+alpha@beta", "1.2.3+alpha beta", "1.2.3+alpha[beta]"
    )
    fun versionValidationThrowTest(version: String) {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "$version"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, version)
        assertEquals(exception1.message, "Invalid value '§sb$version§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [3, 5]     {[vers "$version"]})
                           ^~ 
                [3, 12]     {[vers "$version"]})
                                   ^${"~".repeat(version.length)}~ Invalid value '§sb$version§sr'
            """.trimIndent().fmt
        )
    }

    @ParameterizedTest
    @DisplayName("Ошибка валидации версии зависимости")
    @CsvSource(
        ">>=1.0.0", "<==1.0.0", "^^1.0.0", "~~1.0.0",
        "=!1.0.0", "!>1.0.0", "<>1.0.0",
        "1.0.0 - ", " - 2.0.0", "1.0.0 -  - 2.0.0", "1.0.0 -- 2.0.0",
        "1..0.0", ".1.0.0", "1.0.0.", "1 .0.0", "1. 0.0",
        "'1,0,0'", "1-0-0", "1_0_0", "1:0:0"
    )
    fun dependencyVersionValidationThrowTest(version: String) {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                {[deps [(module
                    {[name "pht/depend"]}
                    {[vers "$version"]})]]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, version)
        assertEquals(exception1.message, "Invalid value '§sb$version§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [4, 5]     {[deps [(module
                           ^~ 
                [4, 13]     {[deps [(module
                                    ^ 
                [6, 9]         {[vers "$version"]})]]})
                               ^~ 
                [6, 16]         {[vers "$version"]})]]})
                                       ^${"~".repeat(version.length)}~ Invalid value '§sb${version}§sr'
            """.trimIndent().fmt
        )
    }

    @ParameterizedTest
    @DisplayName("Ошибка валидации путей исходного кода")
    @CsvSource(
        "../etc/passwd", "../../../../etc/passwd", "..\\..\\..\\windows\\system32\\drivers\\etc\\hosts", "..\\/..\\/etc\\/passwd",
        "%2e%2e%2f%2e%2e%2fetc%2fpasswd", "%252e%252e%252fetc%252fpasswd", "..%2f..%2f..%2fetc%2fpasswd",
        "%c0%ae%c0%ae%c0%afetc%c0%afpasswd", "%e0%80%ae%e0%80%ae/etc/passwd", "%uff0e%uff0e/etc/passwd",
        "/etc/passwd", "C:\\Windows\\System32\\drivers\\etc\\hosts",
        "../../../etc/passwd%00.jpg", "../../etc/passwd\\0", "../../../etc/passwd\\x00.png",
        ".../.../etc/passwd", "....//....//etc//passwd", "..;/../etc/passwd", "..\\..\\..\\/",
        "images/../../../etc/passwd", "uploads/..%2f..%2f..%2fetc%2fpasswd",
        "\\\\?\\C:\\Windows\\System32\\drivers\\etc\\hosts", "..\\..\\..\\..\\..:..\\..\\..\\..\\..\\etc\\passwd"
    )
    fun sourceValidationThrowTest(path: String) {
        val path1 = path.replace("\\", "\\\\")
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                {[src ["$path1"]]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, path)
        assertEquals(exception1.message, "Invalid value '§sb$path§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [4, 5]     {[src ["$path1"]]})
                           ^~ 
                [4, 12]     {[src ["$path1"]]})
                                   ^${"~".repeat(path1.length)}~ Invalid value '§sb$path§sr'
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка неинициализированного обязательного поля")
    fun notInitializedFieldThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.NotInitializedException>(exception0)
        assertEquals(exception1.field, "version")
        assertEquals(exception1.message, "Uninitialized required field '§sbversion§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ Uninitialized required field '§sbversion§sr'
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка неожиданного токена")
    fun unexpectedTokenThrowTest() {
        val parser = Parser(Lexer("([])"))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.UnexpectedToken>(exception0)
        assertEquals(exception1.accepted.type, Token.Type.LIST_START)
        assertEquals(exception1.expected, listOf(Token.Type.ID_MODULE))
        assertEquals(exception1.message, "Unexpected token '§sbLIST_START§sr', expected tokens of type: §sb['ID_MODULE']".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] ([])
                       ^ 
                [1, 2] ([])
                        ^ Unexpected token '§sbLIST_START§sr'
                          Expected tokens of type: §sb['ID_MODULE']
            """.trimIndent().fmt
        )
    }

    inline fun <reified Cause> assertCauses(actual: Throwable): Cause =
        tryAssertCauses<Cause>(actual) ?: throw AssertionError("${actual::class.java} not contains ${Cause::class.java} in causes", actual)

    inline fun <reified Cause> tryAssertCauses(actual: Throwable): Cause? {
        var last = actual
        while (true) {
            if (last is Cause)
                return last
            if (last.cause == null || last.cause == last)
                return null
            last = last.cause!!
        }
    }
}