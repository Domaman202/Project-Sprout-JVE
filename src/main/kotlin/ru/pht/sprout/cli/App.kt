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
    {[name "123"]}  ; Отсутствует значение
    {[vers "1.0.0" "extra"]}  ; Лишние токены
    {[deps [pht/core]]}  ; Идентификатор вместо строки
)
        """.trimIndent()))
        try {
            parser.parse()
        } catch (e: ParserException) {
            System.err.println(e.print(parser))
        }
    }
}