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
    class VarRef(val name: ScopedName) : Expression()
    class FunctionCall(val name: ScopedName, val args: List<Expression>) : Expression()

    data class Type(
        val name: ScopedName,
        val generic: Boolean = false,
        val params: List<Type>? = null
    ) : OrchidNode()

    data class ScopedName(val parts: List<String>) : OrchidNode() {
        constructor(name: String) : this(listOf(name))

        override fun toString() = parts.joinToString(".")
    }
}
