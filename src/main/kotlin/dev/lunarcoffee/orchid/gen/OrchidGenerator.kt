package dev.lunarcoffee.orchid.gen

import dev.lunarcoffee.orchid.gen.sem.OrchidSemanticAnalyzer
import dev.lunarcoffee.orchid.parser.OrchidNode
import dev.lunarcoffee.orchid.parser.Parser
import dev.lunarcoffee.orchid.util.exitWithMessage
import java.io.File

class OrchidGenerator(override val parser: Parser, override val output: File) : Generator {
    private val tree = parser.getTree() as OrchidNode.Program

    override fun gen() {
        OrchidSemanticAnalyzer(tree).verify()

        var text = ""
        for (decl in tree.decls)
            text += declaration(decl)
        for (statement in tree.runnables)
            text += statement(statement)

        output.writeText(text)
    }

    private fun declaration(decl: OrchidNode.TopLevelDecl): String {
        return when (decl) {
            is OrchidNode.FunctionDefinition -> """
                |function ${decl.name}(${decl.args.keys.joinToString(",")}) 
                |{${decl.body.joinToString("") { statement(it) }}}
            """
            else -> exitWithMessage("Syntax: expected function definition!", 3)
        }.trimMargin()
    }

    private fun statement(stmt: OrchidNode.Statement): String {
        return when (stmt) {
            is OrchidNode.VarDecl -> "var ${stmt.name}" +
                    if (stmt.value != null) " = ${expression(stmt.value)};" else ";"
            is OrchidNode.Return -> "return ${expression(stmt.value)};"
            is OrchidNode.Expression -> expression(stmt) + ";"
            is OrchidNode.Scope -> "{${stmt.body.joinToString("") { statement(it) }}}"
            is OrchidNode.IfStatement ->
                "if (${expression(stmt.condition)}) ${statement(stmt.body)}" +
                        if (stmt.elseStmt != null) " else ${statement(stmt.elseStmt)}" else ""
            else -> exitWithMessage("Syntax: expected variable declaration or return!", 3)
        }
    }

    private fun expression(expr: OrchidNode.Expression): String {
        return when (expr) {
            is OrchidNode.NumberLiteral -> expr.value.toString()
            is OrchidNode.StringLiteral -> "\"${expr.value}\""
            is OrchidNode.ArrayLiteral -> "[${joinExpr(expr.values)}]"
            is OrchidNode.BoolTrue -> "true"
            is OrchidNode.BoolFalse -> "false"
            is OrchidNode.VarRef -> expr.name.toString()
            is OrchidNode.Exponent ->
                "Math.pow(${expression(expr.left)}, ${expression(expr.right)})"
            is OrchidNode.BinOp ->
                "(${expression(expr.left)}) ${expr.repr} (${expression(expr.right)})"
            is OrchidNode.UnaryOp -> "${expr.repr}${expression(expr.operand)}"
            is OrchidNode.Assignment -> "${expr.name} = ${expression(expr.value)}"
            is OrchidNode.FunctionCall -> {
                if (expr.name.parts[0] == "js")
                    "${expr.name.parts.drop(1).joinToString(".")}(${joinExpr(expr.args)})"
                else
                    "${expr.name}(${joinExpr(expr.args)})"
            }
            else -> exitWithMessage(
                "Syntax: expected number, string, array, variable, or function call!",
                3
            )
        }
    }

    private fun joinExpr(items: List<OrchidNode.Expression>): String {
        return items.joinToString(",") { expression(it) }
    }
}
