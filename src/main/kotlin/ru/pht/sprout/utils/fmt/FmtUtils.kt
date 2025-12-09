package ru.pht.sprout.utils.fmt

val String.fmt: String get() {
    val out = StringBuilder()
    var i = 0
    while (i < this.length) {
        val char = this[i++]
        out.append(
            if (char == '§') {
                when (val char = this[i++]) {
                    '§' -> out.append(char)
                    'f' -> {
                        when (val char = this[i++]) {
                            '0' -> FmtColor.BLACK
                            '1' -> FmtColor.RED
                            '2' -> FmtColor.GREEN
                            '3' -> FmtColor.YELLOW
                            '4' -> FmtColor.BLUE
                            '5' -> FmtColor.MAGENTA
                            '6' -> FmtColor.CYAN
                            '7' -> FmtColor.WHITE
                            '8' -> FmtColor.BRIGHT_BLACK
                            '9' -> FmtColor.BRIGHT_RED
                            'a' -> FmtColor.BRIGHT_GREEN
                            'b' -> FmtColor.BRIGHT_YELLOW
                            'c' -> FmtColor.BRIGHT_BLUE
                            'd' -> FmtColor.BRIGHT_MAGENTA
                            'e' -> FmtColor.BRIGHT_CYAN
                            'f' -> FmtColor.BRIGHT_WHITE
                            else -> throw RuntimeException("Неожиданный цветовой код текста '$char'")
                        }.fg
                    }

                    'b' -> {
                        when (val char = this[i++]) {
                            '0' -> FmtColor.BLACK
                            '1' -> FmtColor.RED
                            '2' -> FmtColor.GREEN
                            '3' -> FmtColor.YELLOW
                            '4' -> FmtColor.BLUE
                            '5' -> FmtColor.MAGENTA
                            '6' -> FmtColor.CYAN
                            '7' -> FmtColor.WHITE
                            '8' -> FmtColor.BRIGHT_BLACK
                            '9' -> FmtColor.BRIGHT_RED
                            'a' -> FmtColor.BRIGHT_GREEN
                            'b' -> FmtColor.BRIGHT_YELLOW
                            'c' -> FmtColor.BRIGHT_BLUE
                            'd' -> FmtColor.BRIGHT_MAGENTA
                            'e' -> FmtColor.BRIGHT_CYAN
                            'f' -> FmtColor.BRIGHT_WHITE
                            else -> throw RuntimeException("Неожиданный цветовой код фона '$char'")
                        }.bg
                    }

                    's' -> {
                        when (val char = this[i++]) {
                            'r' -> FmtStyle.RESET
                            'b' -> FmtStyle.BOLD
                            'i' -> FmtStyle.ITALIC
                            'u' -> FmtStyle.UNDERLINE
                            else -> throw RuntimeException("Неожиданный код стиля '$char'")
                        }.text
                    }

                    else -> throw RuntimeException("Неожиданный код форматирования '$char'")
                }
            } else char
        )
    }
    return out.append(FmtStyle.RESET.text).toString()
}