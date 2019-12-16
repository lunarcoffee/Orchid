package dev.lunarcoffee.orchid.gen.sem.checker

import dev.lunarcoffee.orchid.gen.sem.SymbolTable
import dev.lunarcoffee.orchid.parser.OrchidNode

class TypeInferrer(override val symbols: SymbolTable) : Checker() {
    fun inferDecl(decl: OrchidNode.VarDecl) = decl.type ?: decl.value?.exprType()
    fun inferExpr(expr: OrchidNode.Expression) = expr.type ?: expr.exprType()
}
