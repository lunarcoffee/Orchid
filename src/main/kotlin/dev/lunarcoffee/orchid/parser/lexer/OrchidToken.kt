package dev.lunarcoffee.orchid.parser.lexer

sealed class OrchidToken(private val repr: String) : Token {
    open class ID(val value: String) : OrchidToken(value)
    class NumberLiteral(val value: Double) : OrchidToken(value.toString())
    class StringLiteral(val value: String) : OrchidToken(value)

    object Colon : OrchidToken(":")
    object Dot : OrchidToken(".")
    object Comma : OrchidToken(",")
    object Equals : OrchidToken("=")
    object Terminator : OrchidToken(";")
    object LBrace : OrchidToken("{")
    object RBrace : OrchidToken("}")
    object LParen : OrchidToken("(")
    object RParen : OrchidToken(")")
    object LBracket : OrchidToken("[")
    object RBracket : OrchidToken("]")
    object EOF : OrchidToken("<EOF>")
    object RArrow : OrchidToken("->")

    // [right] determines right-associativity (i.e. exponentiation).
    open class Operator(repr: String, val precedence: Int, val right: Boolean) : OrchidToken(repr) {
        var assignment = false
    }

    class Plus : Operator("+", 11, false)
    class Dash : Operator("-", 11, false)
    class Asterisk : Operator("*", 12, false)
    class Slash : Operator("/", 12, false)
    class Percent : Operator("%", 12, false)
    class DoubleAsterisk : Operator("**", 13, true)
    class Ampersand : Operator("&", 7, false)
    class Caret : Operator("^", 6, false)
    class Pipe : Operator("|", 5, false)
    class LAngle : Operator("<", 9, false)
    class RAngle : Operator(">", 9, false)
    class DoubleLAngle : Operator("<<", 10, false)
    class DoubleRAngle : Operator(">>", 10, false)
    class RAnglePipe : Operator(">|", 10, false)
    class DoubleEquals : Operator("==", 8, false)
    class BangEquals : Operator("!=", 8, false)
    class LAngleEquals : Operator("<=", 9, false)
    class RAngleEquals : Operator(">=", 9, false)
    class DoubleAmpersand : Operator("&&", 2, false)
    class DoublePipe : Operator("||", 1, false)
    class EqualsDot : Operator("=.", 4, false)
    class In : Operator("in", 3, false)

    object Tilde : Operator("~", -1, false)
    object Bang : Operator("!", -1, false)

    object KVar : ID("var")
    object KFunc : ID("func")
    object KReturn : ID("return")
    object KIf : ID("if")
    object KElse : ID("else")
    object KTrue : ID("true")
    object KFalse : ID("false")
    object KWhen : ID("when")
    object KFor : ID("for")
    object KForEach : ID("foreach")
    object KWhile : ID("while")
    object KExtern : ID("extern")

    override fun toString() = repr
}
