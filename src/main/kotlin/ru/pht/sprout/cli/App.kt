package ru.pht.sprout.cli

import ru.pht.sprout.module.lexer.Lexer
import ru.pht.sprout.module.parser.Parser
import ru.pht.sprout.module.parser.ParserException

object App {
//    @JvmStatic
//    fun main(args: Array<String>) {
//        val lexer = Lexer("""
//            "(module)"xxx
//        """.trimIndent())
//        try {
//            while (lexer.hasNext()) {
//                lexer.next()
//            }
//        } catch (e: LexerException) {
//            System.err.println(e.print(lexer))
//        }
//    }

    @JvmStatic
    fun main(args: Array<String>) {
        val parser = Parser(Lexer("""
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
			; Разрешаем инъекции в модуль
			{[inject-into allow]}
			; Разрешаем инъекции в зависимости модуля
			{[inject-into-deps allow]}
			; Включаем фичу fast-math
			{[features ["fast-math"]]})
		(module
			; Имя модуля
			{[name "pht/util"]}
			; Версия модуля
			{[vers "1.0.0"]}
			; Выключаем использование [по умолчанию]
			{[uses xxx]}
			; Включаем все адаптеры [по умолчанию]
			{[adapters [*]]}
			; Разрешаем инъекции из модуля
			{[inject-from allow]}
			; Выключаем все фичи
			{[no-features [*]]})]]}

	; Инъекции
	; - Из модуля в зависимые модули
	{[inject-into allow]}
	; - Из модуля в зависимости зависимых модулей
	{[inject-into-deps allow]}
	; - В модуль из зависимостей
	{[inject-from allow]}

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
	{[res ["src/*.png"]]}
	{[plg ["plg/*.pht"]]}

	; Исходный код, ресурсы и код плагинов (конкретные файлы)
	{[src ["src/main.pht"]]}
	{[res ["src/icon.png"]]}
	{[plg ["plg/main.pht"]]}

	; Исходный код, ресурсы и код плагинов (собираемые из стороннего модуля)
	{[src [(module {[name "pht/example/sources"]})]]}
	{[res [(module {[name "pht/example/resources"]})]]}
	{[plg [(module {[name "pht/example/plugin"]})]]})
        """.trimIndent()))
        try {
            parser.parse()
        } catch (e: ParserException) {
            System.err.println(e.print(parser))
        }
    }
}