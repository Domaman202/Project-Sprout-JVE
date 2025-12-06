package ru.pht.sprout.module.parser

import ru.pht.sprout.module.Module
import ru.pht.sprout.module.lexer.Lexer
import java.io.File
import java.io.FileInputStream

object ParserUtils {
    fun parseFile(file: String): Module =
        parseFile(File(file))
    fun parseFile(file: File): Module =
        parseString(FileInputStream(file).readBytes().toString(Charsets.UTF_8))
    fun parseString(code: String): Module =
        Parser(Lexer(code)).parse()
}