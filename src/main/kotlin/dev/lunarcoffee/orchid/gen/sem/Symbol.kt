package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.parser.OrchidNode

interface Symbol {
    val name: OrchidNode.ScopedName
    val type: OrchidNode.Type? // [null] represents a built-in type.
}
