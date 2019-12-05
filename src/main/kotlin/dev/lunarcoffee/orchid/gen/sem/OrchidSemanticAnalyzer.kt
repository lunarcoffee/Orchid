package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.parser.OrchidNode

class OrchidSemanticAnalyzer(override val tree: OrchidNode.Program) : SemanticAnalyzer {
    private val symbols = mutableMapOf<OrchidNode.ScopedName, Symbol>()

    override fun verify() {
        if
    }
}
