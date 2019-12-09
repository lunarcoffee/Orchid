package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.util.exitWithMessage

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
        // Check the argument types, return type, and statements.
        for (type in decl.args.values)
            checkType(type)
        checkType(decl.returnType)

        // Put function arguments in scope before checking semantics of body.
        scope++
        for ((name, type) in decl.args)
            symbols.addSymbol(OrchidSymbol.VarSymbol(OrchidNode.VarDecl(name, null, type), scope))
        for (statement in decl.body)
            statement(statement, if (statement is OrchidNode.Return) decl else null)

        // Remove function arguments from scope.
        scope--
        symbols.removeOutOfScope(scope)
        symbols.addSymbol(
            OrchidSymbol.FuncSymbol(decl, decl.args.values.toList(), scope)
        )
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
        }
    }

    private fun variableDeclaration(stmt: OrchidNode.VarDecl) {
        // Check the initializer and type.
        if (stmt.value != null) {
            expression(stmt.value)

            // Check type of initializer expression and type of variable.
            val exprType = getExprType(stmt.value)
            if (exprType != stmt.type)
                exitWithMessage("Semantic: can't assign '$exprType' to '${stmt.type}'!", 4)
        }

        checkType(stmt.type)
        symbols.addSymbol(OrchidSymbol.VarSymbol(stmt, scope))
    }

    private fun returnStatement(stmt: OrchidNode.Return, func: OrchidNode.FunctionDefinition) {
        expression(stmt.value)

        // Check that the return expression type matches the function's return type.
        val exprType = getExprType(stmt.value)
        if (exprType != func.returnType)
            exitWithMessage("Semantic: can't return '$exprType' as '${func.returnType}'!", 4)
    }

    private fun expression(expr: OrchidNode.Expression) {
        when (expr) {
            is OrchidNode.ArrayLiteral -> arrayLiteral(expr)
            is OrchidNode.VarRef -> if (!symbols.isDefined(expr.name))
                exitWithMessage("Semantic: name '${expr.name}' is not defined!", 4)
            is OrchidNode.Assignment -> assignment(expr)
            is OrchidNode.FunctionCall -> functionCall(expr)
            is OrchidNode.BinOp -> {
                expression(expr.left)
                expression(expr.right)

                // Ensure binary operator operand types match.
                if (getExprType(expr.left) != getExprType(expr.right))
                    exitWithMessage("Semantic: binary operator operand types do not match!", 4)
            }
            is OrchidNode.UnaryOp -> expression(expr.operand)
        }
    }

    private fun scope(stmt: OrchidNode.Scope, func: OrchidNode.FunctionDefinition? = null) {
        scope++
        for (statement in stmt.body)
            statement(statement, func)
        scope--
        symbols.removeOutOfScope(scope)
    }

    private fun ifStatement(
        stmt: OrchidNode.IfStatement,
        func: OrchidNode.FunctionDefinition? = null
    ) {
        expression(stmt.condition)
        if (getExprType(stmt.condition) != OrchidNode.Type.boolean)
            exitWithMessage("Semantic: if statement can only contain a 'Boolean' condition!", 4)

        statement(stmt.body, func)
        if (stmt.elseStmt != null)
            statement(stmt.elseStmt, func)
    }

    private fun arrayLiteral(array: OrchidNode.ArrayLiteral) {
        checkType(array.type!!)

        // Check validity of each element and verify that each is the same type as the array.
        for (element in array.values) {
            expression(element)
            val exprType = getExprType(element)
            if (exprType != array.type.params!![0])
                exitWithMessage("Semantic: '${array.type}' cannot contain '$exprType'!", 4)
        }
    }

    private fun assignment(expr: OrchidNode.Assignment) {
        if (!symbols.isDefined(expr.name))
            exitWithMessage("Semantic: name '${expr.name}' is not defined!", 4)

        // Compare types of the expression to assign and the entity to assign.
        val symbol = symbols[expr.name]!!
        val exprType = getExprType(expr.value)
        if (exprType != symbol.type)
            exitWithMessage("Semantic: can't assign '$exprType' to '${symbol.type}'!", 4)

        expression(expr.value)
    }

    private fun functionCall(expr: OrchidNode.FunctionCall) {
        // Check arguments.
        for (arg in expr.args)
            expression(arg)

        // Don't check anything but argument validity for calls prefixed with "js."
        if (expr.name.parts.first() == "js")
            return

        if (!symbols.isDefined(expr.name))
            exitWithMessage("Semantic: function '${expr.name}' is not defined!", 4)

        // Check parameter list sizes.
        val given = expr.args.size
        val expected = (symbols[expr.name] as? OrchidSymbol.FuncSymbol)?.args?.size
        if (given != expected) {
            exitWithMessage(
                "Semantic: function '${expr.name}' " +
                        "expects $expected arguments, $given given!",
                4
            )
        }
    }

    // Recursively validate existence of a type and its generic type parameters.
    private fun checkType(type: OrchidNode.Type) {
        if (!symbols.isDefined(type.name))
            exitWithMessage("Semantic: type '${type.name}' is not defined!", 4)
        type.params?.forEach { checkType(it) }
    }

    private fun getExprType(expr: OrchidNode.Expression): OrchidNode.Type {
        return expr.type ?: when (expr) {
            is OrchidNode.VarRef -> symbols[expr.name]?.type
            is OrchidNode.FunctionCall -> symbols[expr.name]?.type
            is OrchidNode.BinOp -> {
                val typeLeft = getExprType(expr.left)
                if (typeLeft != getExprType(expr.right))
                    exitWithMessage("Semantic: binary operator operand types do not match!", 4)
                typeLeft
            }
            is OrchidNode.UnaryOp -> getExprType(expr.operand)
            else -> exitWithMessage("Semantic: unexpected expression!", 4)
        }!!
    }
}
