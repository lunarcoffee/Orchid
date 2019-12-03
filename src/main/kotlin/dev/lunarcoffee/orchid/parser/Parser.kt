package dev.lunarcoffee.orchid.parser

import dev.lunarcoffee.orchid.parser.lexer.Lexer

interface Parser {
    val lexer: Lexer

    fun getTree(): Node
}
