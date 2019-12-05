package dev.lunarcoffee.orchid.parser

sealed class OrchidNode : Node {
    class Program(val runnables: List<Statement>, val decls: List<TopLevelDecl>) : OrchidNode()

    open class TopLevelDecl : OrchidNode()
    class FunctionDefinition(
        val name: String,
        val args: Map<String, Type>,
        val body: List<Statement>,
        val returnType: Type
    ) : TopLevelDecl()

    open class Statement : OrchidNode()
    class VarDecl(val name: String, val value: Expression?, val type: Type) : Statement()
    class Return(val value: Expression) : Statement()

    open class Expression : Statement()
    class NumberLiteral(val value: Double) : Expression()
    class StringLiteral(val value: String) : Expression()
    class ArrayLiteral(val values: List<Expression>, val type: Type) : Expression()
    class VarRef(val name: String) : Expression()
    class FunctionCall(val name: String, val args: List<Expression>) : Expression()

    class Type(val name: String, val generic: Boolean = false, val params: List<Type>? = null) :
        OrchidNode()
}
