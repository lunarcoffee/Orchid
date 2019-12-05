package dev.lunarcoffee.orchid.gen

import dev.lunarcoffee.orchid.parser.Parser
import java.io.File

interface Generator {
    val parser: Parser
    val output: File

    fun gen()
}
