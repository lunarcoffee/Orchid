package dev.lunarcoffee.orchid.gen.sem.checker

import dev.lunarcoffee.orchid.gen.sem.SymbolTable
import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.util.exitWithMessage

class ReturnInFunctionChecker(override val symbols: SymbolTable) : Checker() {
    override fun returnStatement(stmt: OrchidNode.Return, func: OrchidNode.FunctionDefinition?) {
        if (func == null)
            exitWithMessage("Semantic: return outside of function!", 4)
    }
}
