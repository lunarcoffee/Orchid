package dev.lunarcoffee.orchid.parser.lexer

import dev.lunarcoffee.orchid.util.SafeFileReader
import dev.lunarcoffee.orchid.util.exitWithMessage
import java.io.File

class OrchidLexer(file: File) : Lexer {
    override val chars = SafeFileReader(file).readText()

    private var pos = 0
    private var curChar = chars[pos]

    override fun next(): Token {
        return when (curChar) {
            in '0'..'9' -> OrchidToken.NumberLiteral(readNumber())
            in 'a'..'z', in 'A'..'Z', '_' -> OrchidToken.ID(readIdentifier())
            '"' -> OrchidToken.StringLiteral(readString())
            ':' -> OrchidToken.Colon
            '.' -> OrchidToken.Dot
            ',' -> OrchidToken.Comma
            '=' -> OrchidToken.Equals
            ';' -> OrchidToken.Terminator
            '{' -> OrchidToken.LBrace
            '}' -> OrchidToken.RBrace
            '(' -> OrchidToken.LParen
            ')' -> OrchidToken.RParen
            '[' -> OrchidToken.LBracket
            ']' -> OrchidToken.RBracket
            '<' -> OrchidToken.LAngle
            '>' -> OrchidToken.RAngle
            '\u0000' -> OrchidToken.EOF
            ' ', '\n' -> advance().run { next() }.also { advance(back = true) }
            else -> exitWithMessage("Syntax: unexpected character '$curChar'!", 2)
        }.also { advance() }
    }

    private fun readNumber(): Double {
        var dotSeen = false
        var number = ""
        while (curChar.isDigit() || curChar == '.' && !dotSeen) {
            number += curChar
            dotSeen = !dotSeen && curChar == '.'
            advance()
        }
        advance(back = true)
        return number.toDouble()
    }

    private fun readString(): String {
        var string = ""
        advance()
        while (curChar != '"') {
            string += curChar
            if (curChar == '\\') {
                advance()
                string += curChar
            }
            advance()
        }
        return string
    }

    private fun readIdentifier(): String {
        var res = ""
        while (curChar.isLetterOrDigit() || curChar == '_') {
            res += curChar
            advance()
        }
        advance(back = true)
        return res
    }

    private fun advance(back: Boolean = false) {
        pos += if (back) -1 else 1
        curChar = if (pos > chars.lastIndex) '\u0000' else chars[pos]
    }
}
