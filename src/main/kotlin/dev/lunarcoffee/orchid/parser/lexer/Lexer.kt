package dev.lunarcoffee.orchid.parser.lexer

interface Lexer {
    val chars: String

    fun next(): Token
}
