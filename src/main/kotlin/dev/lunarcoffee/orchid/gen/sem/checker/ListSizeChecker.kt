package dev.lunarcoffee.orchid.gen.sem.checker

import dev.lunarcoffee.orchid.gen.sem.OrchidSymbol
import dev.lunarcoffee.orchid.gen.sem.SymbolTable
import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.util.exitWithMessage

class ListSizeChecker(override val symbols: SymbolTable) : Checker() {
    override fun functionCall(expr: OrchidNode.FunctionCall) {
        if (expr.name.parts[0] == "js")
            return

        val given = expr.args.size
        val expected = (symbols[expr.name]!! as OrchidSymbol.FuncSymbol).args.size

        if (given != expected) {
            exitWithMessage(
                "Semantic: function '${expr.name}' expects $expected arguments, $given given!",
                4
            )
        }
    }
}
