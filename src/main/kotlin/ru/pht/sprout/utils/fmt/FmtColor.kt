package ru.pht.sprout.utils.fmt

enum class FmtColor(val fg: String, val bg: String) {
    BLACK("${Char(27)}[30m", "${Char(27)}[40m"),
    RED("${Char(27)}[31m", "${Char(27)}[41m"),
    GREEN("${Char(27)}[32m", "${Char(27)}[42m"),
    YELLOW("${Char(27)}[33m", "${Char(27)}[43m"),
    BLUE("${Char(27)}[34m", "${Char(27)}[44m"),
    MAGENTA("${Char(27)}[35m", "${Char(27)}[45m"),
    CYAN("${Char(27)}[36m", "${Char(27)}[46m"),
    WHITE("${Char(27)}[37m", "${Char(27)}[47m"),
    BRIGHT_BLACK("${Char(27)}[90m", "${Char(27)}[100m"),
    BRIGHT_RED("${Char(27)}[91m", "${Char(27)}[101m"),
    BRIGHT_GREEN("${Char(27)}[92m", "${Char(27)}[102m"),
    BRIGHT_YELLOW("${Char(27)}[93m", "${Char(27)}[103m"),
    BRIGHT_BLUE("${Char(27)}[94m", "${Char(27)}[104m"),
    BRIGHT_MAGENTA("${Char(27)}[95m", "${Char(27)}[105m"),
    BRIGHT_CYAN("${Char(27)}[96m", "${Char(27)}[106m"),
    BRIGHT_WHITE("${Char(27)}[97m", "${Char(27)}[107m"),
}
