package dev.lunarcoffee.orchid.gen.sem

import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.util.exitWithMessage

class OrchidSemanticAnalyzer(override val tree: OrchidNode.Program) : SemanticAnalyzer {
    private val symbols = SymbolTable()
    private var scope = 0

    // TODO: Expression type checking.
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
            statement(statement)

        // Remove function arguments from scope.
        scope--
        symbols.removeOutOfScope(scope)
        symbols.addSymbol(
            OrchidSymbol.FuncSymbol(decl, decl.args.values.toList(), scope)
        )
    }

    private fun statement(stmt: OrchidNode.Statement) {
        when (stmt) {
            is OrchidNode.VarDecl -> variableDeclaration(stmt)
            is OrchidNode.Return -> expression(stmt.value)
            is OrchidNode.Expression -> expression(stmt)
        }
    }

    private fun variableDeclaration(stmt: OrchidNode.VarDecl) {
        // Check the initializer and type.
        if (stmt.value != null)
            expression(stmt.value)
        checkType(stmt.type)

        symbols.addSymbol(OrchidSymbol.VarSymbol(stmt, scope))
    }

    private fun expression(expr: OrchidNode.Expression) {
        when (expr) {
            is OrchidNode.ArrayLiteral -> arrayLiteral(expr)
            is OrchidNode.VarRef -> if (!symbols.isDefined(expr.name))
                exitWithMessage("Semantic: name '${expr.name}' is not defined!", 4)
            is OrchidNode.FunctionCall -> functionCall(expr)
        }
    }

    private fun arrayLiteral(array: OrchidNode.ArrayLiteral) {
        checkType(array.type)
        for (element in array.values)
            expression(element)
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
        // TODO: Check expected type parameter list size.
        if (!symbols.isDefined(type.name))
            exitWithMessage("Semantic: type '${type.name}' is not defined!", 4)
        type.params?.forEach { checkType(it) }
    }
}
