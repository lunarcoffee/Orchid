package dev.lunarcoffee.orchid.parser

sealed class OrchidNode : Node {
    class Program(val runnables: List<Statement>, val decls: List<TopLevelDecl>) : OrchidNode()

    open class TopLevelDecl : OrchidNode()
    class FunctionDefinition(
        val name: ScopedName,
        val args: Map<String, Type>,
        val body: List<Statement>,
        val returnType: Type
    ) : TopLevelDecl()

    open class Statement : OrchidNode()

    class VarDecl(val name: String, val value: Expression?, var type: Type? = null) : Statement()
    class Scope(val body: List<Statement>) : Statement()
    class Return(val value: Expression) : Statement()
    class ExternFunction(val func: FunctionDefinition) : Statement()

    class IfStatement(
        val condition: Expression,
        val body: Statement,
        val elseStmt: Statement?
    ) : Statement()

    class WhenStatement(val expr: Expression, val branches: List<WhenBranch>) : Statement()
    open class WhenBranch(val body: Statement) : OrchidNode()
    class WhenEqBranch(val exprs: List<Expression>, body: Statement) : WhenBranch(body)
    class WhenInBranch(val expr: Expression, body: Statement) : WhenBranch(body)
    class WhenElseBranch(body: Statement) : WhenBranch(body)

    class ForStatement(
        val init: VarDecl,
        val cmp: Expression,
        val change: Statement,
        val body: Statement
    ) : Statement()

    class ForEachStatement(val decl: VarDecl, val expr: Expression, val body: Statement) :
        Statement()

    class WhileStatement(val cmp: Expression, val body: Statement) : Statement()

    // [type] used in semantic analysis, if null it can be determined from the symbol table.
    open class Expression(val type: Type?) : Statement()

    class NumberLiteral(val value: Double) : Expression(Type.number)
    class StringLiteral(val value: String) : Expression(Type.string)
    class ArrayLiteral(val values: List<Expression>, type: Type) :
        Expression(Type(ScopedName(listOf("Array")), true, listOf(type)))

    class VarRef(val name: ScopedName) : Expression(null)
    class Assignment(val name: ScopedName, val value: Expression) : Expression(null)
    class FunctionCall(val name: ScopedName, val args: List<Expression>) : Expression(null)

    object BoolTrue : Expression(Type.boolean)
    object BoolFalse : Expression(Type.boolean)

    // [repr] is used during code generation to output the correct operator.
    open class BinOp(
        val left: Expression,
        val right: Expression,
        val repr: String,
        type: Type? = null
    ) : Expression(type) {

        var assignment = false
    }

    class Plus(left: Expression, right: Expression) : BinOp(left, right, "+")
    class Minus(left: Expression, right: Expression) : BinOp(left, right, "-")
    class Multiply(left: Expression, right: Expression) : BinOp(left, right, "*")
    class Divide(left: Expression, right: Expression) : BinOp(left, right, "/")
    class Modulo(left: Expression, right: Expression) : BinOp(left, right, "%")

    class BitAnd(left: Expression, right: Expression) : BinOp(left, right, "&")
    class BitXor(left: Expression, right: Expression) : BinOp(left, right, "^")
    class BitOr(left: Expression, right: Expression) : BinOp(left, right, "|")
    class BitLShift(left: Expression, right: Expression) : BinOp(left, right, "<<")
    class BitRShift(left: Expression, right: Expression) : BinOp(left, right, ">>")
    class BitRShiftPad(left: Expression, right: Expression) : BinOp(left, right, ">>>")

    class ArrayRange(left: Expression, right: Expression) :
        BinOp(left, right, "..", Type(ScopedName(listOf("Array")), true, listOf(Type.number)))

    // [repr] is unused; 'Math.pow' is generated instead for higher compatibility.
    class Exponent(left: Expression, right: Expression) : BinOp(left, right, "")

    open class CondOp(left: Expression, right: Expression, repr: String) :
        BinOp(left, right, repr, Type.boolean)

    class BoolEq(left: Expression, right: Expression) : CondOp(left, right, "===")
    class BoolNotEq(left: Expression, right: Expression) : CondOp(left, right, "!==")
    class BoolLess(left: Expression, right: Expression) : CondOp(left, right, "<")
    class BoolGreater(left: Expression, right: Expression) : CondOp(left, right, ">")
    class BoolLessEq(left: Expression, right: Expression) : CondOp(left, right, "<=")
    class BoolGreaterEq(left: Expression, right: Expression) : CondOp(left, right, ">=")
    class BoolIn(left: Expression, right: Expression) : CondOp(left, right, "")

    open class BoolOp(left: Expression, right: Expression, repr: String) : CondOp(left, right, repr)
    class BoolAnd(left: Expression, right: Expression) : BoolOp(left, right, "&&")
    class BoolOr(left: Expression, right: Expression) : BoolOp(left, right, "||")

    open class UnaryOp(val operand: Expression, val repr: String, type: Type? = null) :
        Expression(type)

    class UnaryMinus(operand: Expression) : UnaryOp(operand, "-")
    class UnaryPlus(operand: Expression) : UnaryOp(operand, "+", Type.number)
    class BitComplement(operand: Expression) : UnaryOp(operand, "~")
    class BoolNot(operand: Expression) : UnaryOp(operand, "!")

    // Optionally generic type.
    data class Type(
        val name: ScopedName,
        val generic: Boolean = false,
        val params: List<Type>? = null
    ) : OrchidNode() {

        override fun toString() = "$name" + if (generic) "<${params!!.joinToString(", ")}>" else ""

        companion object {
            val number = Type(ScopedName(listOf("Number")))
            val string = Type(ScopedName(listOf("String")))
            val boolean = Type(ScopedName(listOf("Boolean")))
            val any = Type(ScopedName(listOf("Any")))
            val void = Type(ScopedName(listOf("Void")))

            fun buildArray(type: Type) = Type(ScopedName(listOf("Array")), true, listOf(type))
        }
    }

    // Scoped name like "console.log".
    data class ScopedName(val parts: List<String>) : OrchidNode() {
        constructor(name: String) : this(listOf(name))

        override fun toString() = parts.joinToString(".")
    }
}
