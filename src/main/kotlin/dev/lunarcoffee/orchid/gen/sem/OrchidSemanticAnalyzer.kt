package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.gen.sem.checker.*
import dev.lunarcoffee.orchid.parser.OrchidNode

class OrchidSemanticAnalyzer(override val tree: OrchidNode.Program) : SemanticAnalyzer {
    private val symbols = SymbolTable()
    private var scope = 0

    // Context-sensitive state.
    private var func: OrchidNode.FunctionDefinition? = null
    private var whenStmt: OrchidNode.WhenStatement? = null

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
                withFuncDef(decl) { statement(statement) }
        }
        symbols.addSymbol(OrchidSymbol.FuncSymbol(decl, decl.args.values.toList(), scope))
    }

    private fun statement(stmt: OrchidNode.Statement) {
        when (stmt) {
            is OrchidNode.VarDecl -> variableDeclaration(stmt)
            is OrchidNode.Return -> returnStatement(stmt)
            is OrchidNode.Expression -> expression(stmt)
            is OrchidNode.Scope -> scope(stmt)
            is OrchidNode.IfStatement -> ifStatement(stmt)
            is OrchidNode.WhenStatement -> whenStatement(stmt)
            is OrchidNode.ForStatement -> forStatement(stmt)
            is OrchidNode.ForEachStatement -> forEachStatement(stmt)
            is OrchidNode.WhileStatement -> whileStatement(stmt)
            is OrchidNode.ExternFunction -> functionDefinition(stmt.func)
        }
    }

    private fun variableDeclaration(stmt: OrchidNode.VarDecl) {
        check { varDecl(stmt) }
        symbols.addSymbol(OrchidSymbol.VarSymbol(stmt, scope))
    }

    private fun returnStatement(stmt: OrchidNode.Return) {
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

    private fun scope(stmt: OrchidNode.Scope) {
        newScope {
            for (statement in stmt.body)
                statement(statement)
        }
    }

    private fun ifStatement(stmt: OrchidNode.IfStatement) {
        expression(stmt.condition)
        check { ifStatement(stmt) }

        newScope { statement(stmt.body) }
        if (stmt.elseStmt != null)
            newScope { statement(stmt.elseStmt) }
    }

    private fun whenStatement(stmt: OrchidNode.WhenStatement) {
        expression(stmt.expr)
        for (branch in stmt.branches)
            withWhenStmt(stmt) { whenBranch(branch) }
    }

    private fun whenBranch(branch: OrchidNode.WhenBranch) {
        check { whenBranch(branch, whenStmt!!) }
        statement(branch.body)
    }

    private fun forStatement(stmt: OrchidNode.ForStatement) {
        newScope {
            variableDeclaration(stmt.init)
            expression(stmt.cmp)
            statement(stmt.change)
            statement(stmt.body)
            check { forStatement(stmt) }
        }
    }

    private fun forEachStatement(stmt: OrchidNode.ForEachStatement) {
        newScope {
            variableDeclaration(stmt.decl)
            expression(stmt.expr)
            statement(stmt.body)
            check { forEachStatement(stmt) }
        }
    }

    private fun whileStatement(stmt: OrchidNode.WhileStatement) {
        newScope {
            expression(stmt.cmp)
            statement(stmt.body)
            check { whileStatement(stmt) }
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
        listOf(
            NameChecker(symbols),
            ReturnInFunctionChecker(symbols),
            TypeChecker(symbols),
            ListSizeChecker(symbols)
        ).forEach(func)
    }

    // Analysis within a new scope, automatically removing symbols that go out of scope.
    private fun newScope(analysis: () -> Unit) {
        scope++
        analysis()
        scope--
        symbols.removeOutOfScope(scope)
    }

    private fun withFuncDef(func: OrchidNode.FunctionDefinition, analysis: () -> Unit) {
        this.func = func
        analysis()
        this.func = null
    }

    private fun withWhenStmt(stmt: OrchidNode.WhenStatement, analysis: () -> Unit) {
        whenStmt = stmt
        analysis()
        whenStmt = null
    }
}
