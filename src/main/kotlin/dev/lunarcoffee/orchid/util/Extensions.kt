package dev.lunarcoffee.orchid.util

import kotlin.system.exitProcess

fun exitWithMessage(msg: String, status: Int): Nothing {
    System.err.println(msg)
    exitProcess(status)
}
