package dev.lunarcoffee.orchid.gen.sem.checker

import dev.lunarcoffee.orchid.gen.sem.OrchidSymbol
import dev.lunarcoffee.orchid.gen.sem.SymbolTable
import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.util.exitWithMessage

class TypeChecker(override val symbols: SymbolTable) : Checker() {
    override fun varDecl(decl: OrchidNode.VarDecl) {
        decl.value?.apply { exitNotMatching(exprType(), decl.type) }
    }

    override fun returnStatement(stmt: OrchidNode.Return, func: OrchidNode.FunctionDefinition) {
        exitNotMatching(stmt.value.exprType(), func.returnType)
    }

    override fun ifStatement(stmt: OrchidNode.IfStatement) {
        exitNotMatching(stmt.condition.exprType(), OrchidNode.Type.boolean)
    }

    override fun whenBranch(branch: OrchidNode.WhenBranch, stmt: OrchidNode.WhenStatement) {
        val stmtType = stmt.expr.exprType()
        when (branch) {
            is OrchidNode.WhenEqBranch -> for (expr in branch.exprs)
                exitNotMatching(stmtType, expr.exprType())
            is OrchidNode.WhenInBranch -> exitNotMatching(
                stmtType,
                branch.expr.exprType().params?.get(0)
            )
        }
    }

    override fun forStatement(stmt: OrchidNode.ForStatement) {
        exitNotMatching(stmt.cmp.exprType(), OrchidNode.Type.boolean)
    }

    override fun forEachStatement(stmt: OrchidNode.ForEachStatement) {
        exitNotMatching(stmt.decl.type, stmt.expr.exprType().params?.get(0))
        if (stmt.decl.value != null)
            exitWithMessage("Semantic: 'foreach' variable declaration cannot have value!", 4)
    }

    override fun assignment(expr: OrchidNode.Assignment) {
        exitNotMatching(symbols[expr.name]?.type!!, expr.value.exprType())
    }

    override fun functionCall(expr: OrchidNode.FunctionCall) {
        if (expr.name.parts[0] == "js")
            return

        val function = symbols[expr.name]!! as OrchidSymbol.FuncSymbol
        for ((provided, defined) in expr.args.zip(function.args))
            exitNotMatching(provided.exprType(), defined)
    }

    override fun arrayLiteral(expr: OrchidNode.ArrayLiteral) {
        for (element in expr.values)
            exitNotMatching(element.exprType(), expr.type?.params?.get(0))
    }

    override fun binOp(expr: OrchidNode.BinOp) {
        exitNotMatching(expr.left.exprType(), expr.right.exprType())
        if (expr is OrchidNode.ArrayRange)
            exitNotMatching(expr.left.exprType(), OrchidNode.Type.number)
    }

    override fun condOp(expr: OrchidNode.CondOp) {
        val leftType = expr.left.exprType()
        if (expr is OrchidNode.BoolIn)
            return exitNotMatching(leftType, expr.right.exprType().params?.get(0))

        binOp(expr)
        if (expr is OrchidNode.BoolOp)
            exitNotMatching(leftType, OrchidNode.Type.boolean)
    }

    override fun unaryOp(expr: OrchidNode.UnaryOp) {
        if (expr is OrchidNode.BoolNot)
            exitNotMatching(expr.operand.exprType(), OrchidNode.Type.boolean)
    }

    private fun exitNotMatching(t1: OrchidNode.Type?, t2: OrchidNode.Type?) {
        if (t1 == null || t2 == null)
            exitWithMessage("Semantic: incorrect types!", 4)
        if (t1 != OrchidNode.Type.any && t2 != OrchidNode.Type.any && t1 != t2)
            exitWithMessage("Semantic: '$t1' does not match '$t2'!", 4)
    }
}
