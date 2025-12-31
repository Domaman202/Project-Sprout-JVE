package ru.pht.sprout.cli.shell

import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.TranslationKey
import ru.pht.sprout.utils.exception.SproutIllegalStateException
import java.io.BufferedOutputStream
import java.io.PrintStream

/**
 * Wrapper для [System.out] необходимый для корректной печати указателя ввода команды.
 */
object ShellPrintWrapper {
    private var printer: Printer? = null
    private var cursor: Boolean = false
    private var flush: Boolean = false

    /**
     * Чтение ввода строки.
     *
     * @return Строка.
     */
    fun readln(): String {
        synchronized(this) {
            printer ?: throw SproutIllegalStateException(TranslationKey.of<ShellPrintWrapper>("notInitialized"))
            printer!!.print("§sb§f8> ".fmt)
            cursor = true
        }
        val read = kotlin.io.readln()
        synchronized(this) {
            cursor = false
            if (flush) {
                printer!!.flush()
            }
        }
        return read
    }

    /**
     * Установка [System.out] в [ShellPrintWrapper].
     */
    fun setSystemOut() {
        synchronized(this) {
            if (System.out !is Printer) {
                Printer(Buffer(System.out)).let {
                    this.printer = it
                    System.setOut(it)
                    it.flush()
                }
            }
        }
    }

    /**
     * Сброс [System.out] обратно.
     */
    fun restoreSystemOut() {
        synchronized(this) {
            if (System.out == printer) {
                System.setOut(printer!!.original.original)
                printer!!.close()
                printer = null
            }
        }
    }

    private class Printer(out: Buffer) : PrintStream(out, true) {
        val original: Buffer get() = this.out as Buffer
    }

    private class Buffer(out: PrintStream) : BufferedOutputStream(out) {
        val original: PrintStream get() = this.out as PrintStream

        @Synchronized
        override fun flush() {
            synchronized(ShellPrintWrapper) {
                if (cursor) {
                    flush = true
                } else {
                    flush = false
                    super.flush()
                }
            }
        }
    }
}
