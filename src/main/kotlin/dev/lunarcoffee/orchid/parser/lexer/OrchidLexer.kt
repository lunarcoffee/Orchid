package dev.lunarcoffee.orchid.parser.lexer

import dev.lunarcoffee.orchid.parser.lexer.OrchidToken.*
import dev.lunarcoffee.orchid.util.SafeFileReader
import dev.lunarcoffee.orchid.util.exitWithMessage
import java.io.File
import java.util.*

class OrchidLexer(file: File) : Lexer {
    override val chars = SafeFileReader(file).readText()

    private var pos = 0
    private var curChar = chars[pos]

    private val peekReturns: Queue<Token> = LinkedList<Token>()

    override fun next(): Token {
        if (peekReturns.isNotEmpty())
            return peekReturns.poll()

        return when (curChar) {
            in 'a'..'z', in 'A'..'Z', '_' -> readIdOrKeywordToken()
            in '0'..'9' -> NumberLiteral(readNumber())
            '"' -> StringLiteral(readString())
            ':' -> Colon
            '.' -> Dot
            ',' -> Comma
            ';' -> Terminator
            '{' -> LBrace
            '}' -> RBrace
            '(' -> LParen
            ')' -> RParen
            '[' -> LBracket
            ']' -> RBracket
            '+' -> Plus
            '-' -> ifNextChar('>', RArrow, Dash)
            '*' -> ifNextChar('*', DoubleAsterisk, Asterisk)
            '/' -> Slash
            '%' -> Percent
            '&' -> ifNextChar('&', DoubleAmpersand, Ampersand)
            '^' -> Caret
            '|' -> ifNextChar('|', DoublePipe, Pipe)
            '~' -> Tilde
            '!' -> ifNextChar('=', BangEquals, Bang)
            '\u0000' -> EOF
            ' ', '\n' -> advance().run { next() }.also { advance(back = true) }
            '=' -> ifNextChar('=', DoubleEquals, ifNextChar('.', EqualsDot, Equals))
            '<' -> ifNextChar('=', LAngleEquals, ifNextChar('<', DoubleLAngle, LAngle))
            '>' -> ifNextChar(
                '=',
                RAngleEquals,
                ifNextChar('>', DoubleRAngle, ifNextChar('|', RAnglePipe, RAngle))
            )
            '#' -> {
                while (curChar != '\n')
                    advance()
                next()
            }
            else -> exitWithMessage("Syntax: unexpected character '$curChar'!", 2)
        }.also { advance() }
    }

    override fun peek() = next().also { peekReturns.offer(it) }

    private fun ifNextChar(nextChar: Char, ifTrue: Token, ifFalse: Token): Token {
        advance()
        if (curChar == nextChar)
            return ifTrue
        advance(back = true)
        return ifFalse
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

    private fun readIdOrKeywordToken(): Token {
        var res = ""
        while (curChar.isLetterOrDigit() || curChar == '_') {
            res += curChar
            advance()
        }
        advance(back = true)
        return keywords.getOrElse(res) { ID(res) }
    }

    private fun advance(back: Boolean = false) {
        pos += if (back) -1 else 1
        curChar = if (pos > chars.lastIndex) '\u0000' else chars[pos]
    }

    companion object {
        private val keywords = mapOf(
            "var" to KVar,
            "func" to KFunc,
            "return" to KReturn,
            "if" to KIf,
            "else" to KElse,
            "true" to KTrue,
            "false" to KFalse,
            "when" to KWhen,
            "in" to KIn
        )
    }
}
