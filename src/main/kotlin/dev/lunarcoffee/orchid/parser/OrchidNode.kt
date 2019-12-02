package dev.lunarcoffee.orchid.parser

sealed class OrchidNode : Node {
    class FunctionDefinition<T>(
        val name: String,
        val args: List<Expression<*>>,
        val body: List<Statement>,
        val returnType: T
    )

    open class Statement : OrchidNode()
    class VarDecl<T>(val name: String, val value: T) : Statement()
    class Return<T>(val value: Expression<T>) : Statement()

    open class Expression<T>(val value: T) : Statement()
    class NumberLiteral(value: Double) : Expression<Double>(value)
    class StringLiteral(value: String) : Expression<String>(value)
    class ArrayLiteral<T : Expression<T>>(values: List<T>) : Expression<List<T>>(values)
    class VarRef<T>(val name: String, value: T) : Expression<T>(value)
    class FunctionCall<T>(val name: String, value: T, val args: List<Expression<*>>) :
        Expression<T>(value)
}
