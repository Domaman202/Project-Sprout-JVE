package ru.pht.sprout.utils.fmt

enum class FmtStyle(val text: String) {
    RESET("${Char(27)}[00m"),
    BOLD("${Char(27)}[01m"),
    ITALIC("${Char(27)}[03m"),
    UNDERLINE("${Char(27)}[04m"),
}