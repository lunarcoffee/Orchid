package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.gen.sem.checker.Checker
import dev.lunarcoffee.orchid.gen.sem.checker.ListSizeChecker
import dev.lunarcoffee.orchid.gen.sem.checker.NameChecker
import dev.lunarcoffee.orchid.gen.sem.checker.TypeChecker
import dev.lunarcoffee.orchid.parser.OrchidNode

class OrchidSemanticAnalyzer(override val tree: OrchidNode.Program) : SemanticAnalyzer {
    private val symbols = SymbolTable()
    private var scope = 0

    override fun verify() {
        // Hoist all top-level declarations.
        for (decl in tree.decls)
            if (decl is OrchidNode.FunctionDefinition)
                functionDefinition(decl)
        for (runnable in tree.runnables)
            statement(runnable)
    }

    private fun functionDefinition(decl: OrchidNode.FunctionDefinition) {
        newScope {
            for ((name, type) in decl.args)
                symbols
                    .addSymbol(OrchidSymbol.VarSymbol(OrchidNode.VarDecl(name, null, type), scope))
            check { functionDefinition(decl) }
            for (statement in decl.body)
                statement(statement, if (statement is OrchidNode.Return) decl else null)
        }
        symbols.addSymbol(OrchidSymbol.FuncSymbol(decl, decl.args.values.toList(), scope))
    }

    private fun statement(
        stmt: OrchidNode.Statement,
        func: OrchidNode.FunctionDefinition? = null
    ) {
        when (stmt) {
            is OrchidNode.VarDecl -> variableDeclaration(stmt)
            is OrchidNode.Return -> returnStatement(stmt, func!!)
            is OrchidNode.Expression -> expression(stmt)
            is OrchidNode.Scope -> scope(stmt, func)
            is OrchidNode.IfStatement -> ifStatement(stmt, func)
            is OrchidNode.WhenStatement -> whenStatement(stmt, func)
            is OrchidNode.ForStatement -> forStatement(stmt)
            is OrchidNode.ForEachStatement -> forEachStatement(stmt)
        }
    }

    private fun variableDeclaration(stmt: OrchidNode.VarDecl) {
        check { varDecl(stmt) }
        symbols.addSymbol(OrchidSymbol.VarSymbol(stmt, scope))
    }

    private fun returnStatement(stmt: OrchidNode.Return, func: OrchidNode.FunctionDefinition) {
        expression(stmt.value)
        check { returnStatement(stmt, func) }
    }

    private fun expression(expr: OrchidNode.Expression) {
        when (expr) {
            is OrchidNode.ArrayLiteral -> arrayLiteral(expr)
            is OrchidNode.Assignment -> assignment(expr)
            is OrchidNode.FunctionCall -> functionCall(expr)
            is OrchidNode.ArrayRange -> binOp(expr)
            is OrchidNode.CondOp -> condOp(expr)
            is OrchidNode.BinOp -> binOp(expr)
            is OrchidNode.UnaryOp -> unaryOp(expr)
            is OrchidNode.VarRef -> check { varRef(expr) }
        }
    }

    private fun condOp(expr: OrchidNode.CondOp) {
        expression(expr.left)
        expression(expr.right)
        check { condOp(expr) }
    }

    private fun binOp(expr: OrchidNode.BinOp) {
        expression(expr.left)
        expression(expr.right)
        check { binOp(expr) }
    }

    private fun unaryOp(expr: OrchidNode.UnaryOp) {
        expression(expr.operand)
        check { unaryOp(expr) }
    }

    private fun scope(stmt: OrchidNode.Scope, func: OrchidNode.FunctionDefinition? = null) {
        newScope {
            for (statement in stmt.body)
                statement(statement, func)
        }
    }

    private fun ifStatement(
        stmt: OrchidNode.IfStatement,
        func: OrchidNode.FunctionDefinition? = null
    ) {
        expression(stmt.condition)
        check { ifStatement(stmt) }

        newScope { statement(stmt.body, func) }
        if (stmt.elseStmt != null)
            newScope { statement(stmt.elseStmt, func) }
    }

    private fun whenStatement(
        stmt: OrchidNode.WhenStatement,
        func: OrchidNode.FunctionDefinition? = null
    ) {
        expression(stmt.expr)
        for (branch in stmt.branches)
            whenBranch(branch, stmt, func)
    }

    private fun whenBranch(
        branch: OrchidNode.WhenBranch,
        stmt: OrchidNode.WhenStatement,
        func: OrchidNode.FunctionDefinition? = null
    ) {
        check { whenBranch(branch, stmt) }
        statement(branch.body, func)
    }

    private fun forStatement(
        stmt: OrchidNode.ForStatement,
        func: OrchidNode.FunctionDefinition? = null
    ) {
        newScope {
            variableDeclaration(stmt.init)
            expression(stmt.cmp)
            statement(stmt.change, func)
            statement(stmt.body, func)
            check { forStatement(stmt) }
        }
    }

    private fun forEachStatement(
        stmt: OrchidNode.ForEachStatement,
        func: OrchidNode.FunctionDefinition? = null
    ) {
        newScope {
            variableDeclaration(stmt.decl)
            expression(stmt.expr)
            statement(stmt.body, func)
            check { forEachStatement(stmt) }
        }
    }

    private fun arrayLiteral(array: OrchidNode.ArrayLiteral) {
        for (element in array.values)
            expression(element)
        check { arrayLiteral(array) }
    }

    private fun assignment(expr: OrchidNode.Assignment) {
        expression(expr.value)
        check { assignment(expr) }
    }

    private fun functionCall(expr: OrchidNode.FunctionCall) {
        for (arg in expr.args)
            expression(arg)
        check { functionCall(expr) }
    }

    private fun check(func: Checker.() -> Unit) {
        val checkers = listOf(NameChecker(symbols), TypeChecker(symbols), ListSizeChecker(symbols))
        for (checker in checkers)
            checker.func()
    }

    private fun newScope(func: () -> Unit) {
        scope++
        func()
        scope--
        symbols.removeOutOfScope(scope)
    }
}
