package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.parser.OrchidNode

sealed class OrchidSymbol(
    override val name: OrchidNode.ScopedName,
    override val type: OrchidNode.Type?,
    override val scope: Int
) : Symbol {

    constructor(name: String, scope: Int) : this(OrchidNode.ScopedName(listOf(name)), null, scope)

    class VarSymbol(variable: OrchidNode.VarDecl, scope: Int) :
        OrchidSymbol(OrchidNode.ScopedName(variable.name), variable.type, scope)

    class FuncSymbol(
        call: OrchidNode.FunctionDefinition,
        val args: List<OrchidNode.Type>,
        scope: Int
    ) : OrchidSymbol(call.name, call.returnType, scope)

    open class BuiltinSymbol(name: String) : OrchidSymbol(name, 0)
    object NumberType : BuiltinSymbol("Number")
    object StringType : BuiltinSymbol("String")
    object BooleanType : BuiltinSymbol("Boolean")
    object ArrayType : BuiltinSymbol("Array")
    object AnyType : BuiltinSymbol("Any")
    object VoidType : BuiltinSymbol("Void")
}
