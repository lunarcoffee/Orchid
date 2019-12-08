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

    // [type] used in semantic analysis, if null it can be determined from the symbol table.
    open class Expression(val type: Type?) : Statement()

    class NumberLiteral(val value: Double) : Expression(Type(ScopedName(listOf("Number"))))
    class StringLiteral(val value: String) : Expression(Type(ScopedName(listOf("String"))))
    class ArrayLiteral(val values: List<Expression>, type: Type) :
        Expression(Type(ScopedName(listOf("Array")), true, listOf(type)))

    class VarRef(val name: ScopedName) : Expression(null)
    class Assignment(val name: ScopedName, val value: Expression) : Expression(null)
    class FunctionCall(val name: ScopedName, val args: List<Expression>) : Expression(null)

    // Optionally generic type.
    data class Type(
        val name: ScopedName,
        val generic: Boolean = false,
        val params: List<Type>? = null
    ) : OrchidNode() {

        override fun toString() = "$name" + if (generic) "<${params!!.joinToString(", ")}>" else ""
    }

    // Scoped name like "console.log".
    data class ScopedName(val parts: List<String>) : OrchidNode() {
        constructor(name: String) : this(listOf(name))

        override fun toString() = parts.joinToString(".")
    }
}
