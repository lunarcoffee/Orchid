package dev.lunarcoffee.orchid.gen.sem.checker

import dev.lunarcoffee.orchid.gen.sem.SymbolTable
import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.util.exitWithMessage

abstract class Checker {
    abstract val symbols: SymbolTable

    open fun functionDefinition(func: OrchidNode.FunctionDefinition) {}
    open fun varDecl(decl: OrchidNode.VarDecl) {}
    open fun returnStatement(stmt: OrchidNode.Return, func: OrchidNode.FunctionDefinition) {}
    open fun ifStatement(stmt: OrchidNode.IfStatement) {}
    open fun whenBranch(branch: OrchidNode.WhenBranch, stmt: OrchidNode.WhenStatement) {}
    open fun forStatement(stmt: OrchidNode.ForStatement) {}
    open fun forEachStatement(stmt: OrchidNode.ForEachStatement) {}
    open fun expression(expr: OrchidNode.Expression) {}
    open fun varRef(expr: OrchidNode.VarRef) {}
    open fun assignment(expr: OrchidNode.Assignment) {}
    open fun functionCall(expr: OrchidNode.FunctionCall) {}
    open fun arrayLiteral(expr: OrchidNode.ArrayLiteral) {}
    open fun binOp(expr: OrchidNode.BinOp) {}
    open fun condOp(expr: OrchidNode.CondOp) {}
    open fun unaryOp(expr: OrchidNode.UnaryOp) {}

    // Recursively validate existence of a type and its generic type parameters.
    protected fun OrchidNode.Type.checkType() {
        if (!symbols.isDefined(name))
            exitWithMessage("Semantic: type '$name' is not defined!", 4)
        params?.forEach { it.checkType() }
    }

    protected fun OrchidNode.Expression.exprType(): OrchidNode.Type {
        return type ?: when (this) {
            is OrchidNode.VarRef -> symbols[name]?.type
            is OrchidNode.FunctionCall -> symbols[name]?.type
            is OrchidNode.BinOp -> {
                val typeLeft = left.exprType()
                if (typeLeft != right.exprType())
                    exitWithMessage("Semantic: binary operator operand types do not match!", 4)
                typeLeft
            }
            is OrchidNode.UnaryOp -> operand.exprType()
            is OrchidNode.Assignment -> value.exprType()
            else -> exitWithMessage("Semantic: unexpected expression!", 4)
        }!!
    }
}
