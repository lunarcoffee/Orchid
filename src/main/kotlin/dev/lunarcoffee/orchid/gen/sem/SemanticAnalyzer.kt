package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.parser.Node

interface SemanticAnalyzer {
    val tree: Node

    // [verify] should exit and display an error message upon semantic
    fun verify()
}
