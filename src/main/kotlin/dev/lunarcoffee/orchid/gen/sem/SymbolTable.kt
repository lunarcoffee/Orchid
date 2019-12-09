package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.util.exitWithMessage

class SymbolTable : MutableMap<OrchidNode.ScopedName, Symbol> by mutableMapOf() {
    init {
        for (builtin in builtins)
            put(builtin.name, builtin)
    }

    fun addSymbol(symbol: Symbol) {
        if (containsKey(symbol.name))
            exitWithMessage("Semantic: name '${symbol.name}' already defined!", 4)
        put(symbol.name, symbol)
    }

    fun isDefined(name: OrchidNode.ScopedName) = get(name) != null

    fun removeOutOfScope(scope: Int) {
        val toRemove = mutableListOf<OrchidNode.ScopedName>()
        for ((name, symbol) in this)
            if (symbol.scope > scope)
                toRemove += name
        for (name in toRemove)
            remove(name)
    }

    companion object {
        private val builtins = listOf(
            OrchidSymbol.NumberType,
            OrchidSymbol.StringType,
            OrchidSymbol.BooleanType,
            OrchidSymbol.ArrayType,
            OrchidSymbol.AnyType,
            OrchidSymbol.VoidType
        )
    }
}
