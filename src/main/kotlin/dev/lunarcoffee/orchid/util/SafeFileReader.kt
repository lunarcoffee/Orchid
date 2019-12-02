package dev.lunarcoffee.orchid.util

import java.io.File

class SafeFileReader(private val file: File) {
    fun readText(): String {
        if (!file.exists())
            exitWithMessage("Fatal: file at '${file.path}' does not exist!", 1)
        return file.readText()
    }
}
