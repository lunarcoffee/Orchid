package dev.lunarcoffee.orchid.parser

sealed class OrchidNode : Node {
    class Program(val runnables: List<Statement>, val decls: List<TopLevelDecl>) : OrchidNode()

    open class TopLevelDecl : OrchidNode()
    class TopLevelVarDecl(val name: String, val value: Expression?, val type: String) :
        TopLevelDecl()

    class FunctionDefinition<T>(
        val name: String,
        val args: List<Expression>,
        val body: List<Statement>,
        val returnType: T
    ) : TopLevelDecl()

    open class Statement : OrchidNode()
    class VarDecl(val name: String, val value: Expression?, val type: String) : Statement()
    class Return(val value: Expression) : Statement()

    open class Expression : Statement()
    class NumberLiteral(val value: Double) : Expression()
    class StringLiteral(val value: String) : Expression()
    class ArrayLiteral(val values: List<Expression>, val type: String) : Expression()
    class VarRef(val name: String) : Expression()
    class FunctionCall(val name: String, val args: List<Expression>) : Expression()
}
