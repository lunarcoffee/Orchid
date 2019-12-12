package dev.lunarcoffee.orchid.gen.sem.checker

import dev.lunarcoffee.orchid.gen.sem.SymbolTable
import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.util.exitWithMessage

class NameChecker(override val symbols: SymbolTable) : Checker() {
    override fun functionDefinition(func: OrchidNode.FunctionDefinition) {
        func.returnType.checkType()
        for (type in func.args.values)
            type.checkType()
    }

    override fun varDecl(decl: OrchidNode.VarDecl) = decl.type.checkType()

    override fun expression(expr: OrchidNode.Expression) = expr.exprType().checkType()
    override fun varRef(expr: OrchidNode.VarRef) = expr.name.checkName()
    override fun assignment(expr: OrchidNode.Assignment) = expr.name.checkName()

    override fun functionCall(expr: OrchidNode.FunctionCall) {
        if (expr.name.parts[0] == "js")
            return
        expr.name.checkName()
    }

    private fun OrchidNode.ScopedName.checkName() {
        if (!symbols.isDefined(this))
            exitWithMessage("Semantic: name '$this' not in scope!", 4)
    }
}
