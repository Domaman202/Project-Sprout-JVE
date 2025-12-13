package ru.pht.sprout.module.header.lexer

import io.github.z4kn4fein.semver.toVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
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

    @Test
    @DisplayName("Ошибка валидации имени")
    fun nameValidationThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht@example"]}
                {[vers "1.0.0"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, "pht@example")
        assertEquals(exception1.message, "Invalid value '§sbpht@example§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [2, 5]     {[name "pht@example"]}
                           ^~ 
                [2, 12]     {[name "pht@example"]}
                                   ^~~~~~~~~~~~~ Invalid value '§sbpht@example§sr'
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка валидации имени зависимости")
    fun dependencyNameValidationThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                {[deps [(module
                    {[name "pht/]epend"]}
                    {[vers "1.0.0"]})]]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, "pht/]epend")
        assertEquals(exception1.message, "Invalid value '§sbpht/]epend§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [4, 5]     {[deps [(module
                           ^~ 
                [4, 13]     {[deps [(module
                                    ^ 
                [5, 9]         {[name "pht/]epend"]}
                               ^~ 
                [5, 16]         {[name "pht/]epend"]}
                                       ^~~~~~~~~~~~ Invalid value '§sbpht/]epend§sr'
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка валидации версии")
    fun versionValidationThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.$.0"]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, "1.$.0")
        assertEquals(exception1.message, "Invalid value '§sb1.$.0§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [3, 5]     {[vers "1.$.0"]})
                           ^~ 
                [3, 12]     {[vers "1.$.0"]})
                                   ^~~~~~~ Invalid value '§sb1.$.0§sr'
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка валидации версии зависимости")
    fun dependencyVersionValidationThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                {[deps [(module
                    {[name "pht/depend"]}
                    {[vers "#.0.0"]})]]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, "#.0.0")
        assertEquals(exception1.message, "Invalid value '§sb#.0.0§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [4, 5]     {[deps [(module
                           ^~ 
                [4, 13]     {[deps [(module
                                    ^ 
                [6, 9]         {[vers "#.0.0"]})]]})
                               ^~ 
                [6, 16]         {[vers "#.0.0"]})]]})
                                       ^~~~~~~ Invalid value '§sb#.0.0§sr'
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка валидации путей исходного кода")
    fun sourceValidationThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                {[src ["../src/*.pht"]]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, "../src/*.pht")
        assertEquals(exception1.message, "Invalid value '§sb../src/*.pht§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [4, 5]     {[src ["../src/*.pht"]]})
                           ^~ 
                [4, 12]     {[src ["../src/*.pht"]]})
                                   ^~~~~~~~~~~~~~ Invalid value '§sb../src/*.pht§sr'
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка валидации путей ресурсов")
    fun resourceValidationThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                {[res ["/res/icon.png"]]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, "/res/icon.png")
        assertEquals(exception1.message, "Invalid value '§sb/res/icon.png§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [4, 5]     {[res ["/res/icon.png"]]})
                           ^~ 
                [4, 12]     {[res ["/res/icon.png"]]})
                                   ^~~~~~~~~~~~~~~ Invalid value '§sb/res/icon.png§sr'
            """.trimIndent().fmt
        )
    }

    @Test
    @DisplayName("Ошибка валидации путей плагинов")
    fun pluginValidationThrowTest() {
        val parser = Parser(Lexer("""
            (module "pht/module"
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                {[plg ["C:/plg/virus.pht"]]})
        """.trimIndent()))
        val exception0 = assertThrows<ParserException> { parser.parse() }
        val exception1 = assertCauses<ParserException.ValidationException>(exception0)
        assertEquals(exception1.value, "C:/plg/virus.pht")
        assertEquals(exception1.message, "Invalid value '§sbC:/plg/virus.pht§sr'".fmt)
        assertEquals(
            exception0.print(parser, Language.ENGLISH).toString(),
            """
                [1, 1] (module "pht/module"
                       ^ 
                [4, 5]     {[plg ["C:/plg/virus.pht"]]})
                           ^~ 
                [4, 12]     {[plg ["C:/plg/virus.pht"]]})
                                   ^~~~~~~~~~~~~~~~~~ Invalid value '§sbC:/plg/virus.pht§sr'
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

    @Suppress("UNCHECKED_CAST")
    inline fun <reified Cause> assertCauses(actual: Throwable): Cause {
        var last = actual
        while (true) {
            if (last is Cause)
                return last
            if (last.cause == null || last.cause == last)
                throw AssertionError("${actual::class.java} not contains ${Cause::class.java} in causes", actual)
            last = last.cause!!
        }
    }
}