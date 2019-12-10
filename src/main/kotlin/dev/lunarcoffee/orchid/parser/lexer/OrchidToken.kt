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
    object LAngle : OrchidToken("<")
    object RAngle : OrchidToken(">")
    object EOF : OrchidToken("<EOF>")
    object RArrow : OrchidToken("->")

    // [right] determines right-associativity (i.e. exponentiation).
    open class Operator(repr: String, val precedence: Int, val right: Boolean) : OrchidToken(repr)

    object Plus : Operator("+", 9, false)
    object Dash : Operator("-", 9, false)
    object Asterisk : Operator("*", 10, false)
    object Slash : Operator("/", 10, false)
    object Percent : Operator("%", 10, false)
    object DoubleAsterisk : Operator("**", 11, true)
    object Ampersand : Operator("&", 3, false)
    object Caret : Operator("^", 4, false)
    object Pipe : Operator("|", 5, false)
    object DoubleLAngle : Operator("<<", 8, false)
    object DoubleRAngle : Operator(">>", 8, false)
    object RAnglePipe : Operator(">|", 8, false)
    object DoubleEquals : Operator("==", 6, false)
    object BangEquals : Operator("!=", 6, false)
    object LAngleEquals : Operator("<=", 7, false)
    object RAngleEquals : Operator(">=", 7, false)
    object DoubleAmpersand : Operator("&&", 2, false)
    object DoublePipe : Operator("||", 1, false)

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
    object KIn : ID("in")

    override fun toString() = repr
}
