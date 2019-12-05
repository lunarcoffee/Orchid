package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.parser.OrchidNode

sealed class OrchidSymbol(
    override val name: OrchidNode.ScopedName,
    override val type: OrchidNode.Type?
) : Symbol {

    constructor(name: String) : this(OrchidNode.ScopedName(listOf(name)), null)



    object NumberType : OrchidSymbol("Number")
    object StringType : OrchidSymbol("String")
    object VoidType : OrchidSymbol("Void")
}
