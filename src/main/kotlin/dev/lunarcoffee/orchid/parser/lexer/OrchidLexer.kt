package dev.lunarcoffee.orchid.parser.lexer

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
            in '0'..'9' -> OrchidToken.NumberLiteral(readNumber())
            '"' -> OrchidToken.StringLiteral(readString())
            ':' -> OrchidToken.Colon
            '.' -> OrchidToken.Dot
            ',' -> OrchidToken.Comma
            '=' -> ifNextChar('=', OrchidToken.DoubleEquals, OrchidToken.Equals)
            ';' -> OrchidToken.Terminator
            '{' -> OrchidToken.LBrace
            '}' -> OrchidToken.RBrace
            '(' -> OrchidToken.LParen
            ')' -> OrchidToken.RParen
            '[' -> OrchidToken.LBracket
            ']' -> OrchidToken.RBracket
            '+' -> OrchidToken.Plus
            '-' -> ifNextChar('>', OrchidToken.RArrow, OrchidToken.Dash)
            '*' -> ifNextChar('*', OrchidToken.DoubleAsterisk, OrchidToken.Asterisk)
            '/' -> OrchidToken.Slash
            '%' -> OrchidToken.Percent
            '&' -> ifNextChar('&', OrchidToken.DoubleAmpersand, OrchidToken.Ampersand)
            '^' -> OrchidToken.Caret
            '|' -> ifNextChar('|', OrchidToken.DoublePipe, OrchidToken.Pipe)
            '~' -> OrchidToken.Tilde
            '!' -> ifNextChar('=', OrchidToken.BangEquals, OrchidToken.Bang)
            '\u0000' -> OrchidToken.EOF
            ' ', '\n' -> advance().run { next() }.also { advance(back = true) }
            '#' -> {
                while (curChar != '\n')
                    advance()
                next()
            }
            '<' -> ifNextChar(
                '=',
                OrchidToken.LAngleEquals,
                ifNextChar('<', OrchidToken.DoubleLAngle, OrchidToken.LAngle)
            )
            '>' -> ifNextChar(
                '=',
                OrchidToken.RAngleEquals,
                ifNextChar(
                    '>',
                    OrchidToken.DoubleRAngle,
                    ifNextChar('|', OrchidToken.RAnglePipe, OrchidToken.RAngle)
                )
            )
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
        return keywords.getOrElse(res) { OrchidToken.ID(res) }
    }

    private fun advance(back: Boolean = false) {
        pos += if (back) -1 else 1
        curChar = if (pos > chars.lastIndex) '\u0000' else chars[pos]
    }

    companion object {
        private val keywords = mapOf(
            "var" to OrchidToken.KVar,
            "func" to OrchidToken.KFunc,
            "return" to OrchidToken.KReturn,
            "if" to OrchidToken.KIf,
            "else" to OrchidToken.KElse,
            "true" to OrchidToken.KTrue,
            "false" to OrchidToken.KFalse,
            "when" to OrchidToken.KWhen,
            "in" to OrchidToken.KIn
        )
    }
}
