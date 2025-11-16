package ru.pht.sprout.cli

import ru.pht.sprout.module.lexer.Lexer
import ru.pht.sprout.module.lexer.LexerException

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        val lexer = Lexer("    \\  \n   ")
        try {
            while (lexer.hasNext()) {
                println(lexer.nextToken())
            }
        } catch (e: LexerException) {
            System.err.println(e.print(lexer))
        }
    }
}