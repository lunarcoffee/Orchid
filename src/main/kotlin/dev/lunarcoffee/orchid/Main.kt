package dev.lunarcoffee.orchid

import dev.lunarcoffee.orchid.gen.OrchidGenerator
import dev.lunarcoffee.orchid.parser.OrchidParser
import dev.lunarcoffee.orchid.parser.lexer.OrchidLexer
import dev.lunarcoffee.orchid.util.exitWithMessage
import java.io.File

/*
 * TODO:
 *  - Implement control flow analysis to check that all paths return in functions.
 *  - Implement extern declarations.
 *  - Implement multi-file compilation, import system.
 */

fun main(args: Array<String>) {
    val input = args.firstOrNull() ?: exitWithMessage("Fatal: no input file specified!", 1)
    val output = args.getOrNull(1) ?: exitWithMessage("Fatal: no output file specified!", 1)

    val lexer = OrchidLexer(File(input))
//    var t = lexer.next()
//    while (t != OrchidToken.EOF) {
//        println(t)
//        t = lexer.next()
//    }
    val parser = OrchidParser(lexer)
    val gen = OrchidGenerator(parser, File(output))
    gen.gen()
}
