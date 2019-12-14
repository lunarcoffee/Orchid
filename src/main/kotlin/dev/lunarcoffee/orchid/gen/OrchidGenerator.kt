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
            is OrchidNode.FunctionDefinition ->
                "function ${decl.name}(${decl.args.keys.joinToString(",")})" +
                        "{${decl.body.joinToString("") { statement(it) }}}"
            else -> exitWithMessage("Syntax: expected top-level declaration!", 3)
        }.trimMargin()
    }

    private fun statement(stmt: OrchidNode.Statement): String {
        return when (stmt) {
            is OrchidNode.VarDecl -> "var ${stmt.name}" +
                    if (stmt.value != null) "=${expression(stmt.value)};" else ";"
            is OrchidNode.Return -> "return ${expression(stmt.value)};"
            is OrchidNode.Expression -> expression(stmt) + ";"
            is OrchidNode.Scope -> "{${stmt.body.joinToString("") { statement(it) }}}"
            is OrchidNode.IfStatement ->
                "if(${expression(stmt.condition)})${statement(stmt.body)}" +
                        if (stmt.elseStmt != null) "else ${statement(stmt.elseStmt)}" else ""
            is OrchidNode.WhenStatement -> whenStatement(stmt)
            is OrchidNode.ForStatement -> "for(${statement(stmt.init)}${expression(stmt.cmp)};" +
                    "${statement(stmt.change).dropLast(1)})${statement(stmt.body)}"
            is OrchidNode.ForEachStatement -> forEachStatement(stmt)
            is OrchidNode.WhileStatement -> "while(${expression(stmt.cmp)})${statement(stmt.body)}"
            else -> exitWithMessage("Syntax: expected statement!", 3)
        }
    }

    private fun whenStatement(stmt: OrchidNode.WhenStatement): String {
        val expr = expression(stmt.expr)
        val first = stmt.branches.firstOrNull() ?: return ""
        val rest = stmt.branches.drop(1).joinToString("") { whenBranch(it) }
        return "var \$e=$expr;${whenBranch(first, true)}$rest"
    }

    private fun whenBranch(branch: OrchidNode.WhenBranch, first: Boolean = false): String {
        val statement = if (first) "if" else "else if"
        return when (branch) {
            is OrchidNode.WhenEqBranch -> {
                val cmps = branch.exprs.joinToString("||") { "\$e===${expression(it)}" }
                "$statement($cmps)${statement(branch.body)}"
            }
            is OrchidNode.WhenInBranch ->
                "$statement(${expression(branch.expr)}.includes(\$e))${statement(branch.body)}"
            is OrchidNode.WhenElseBranch -> "else ${statement(branch.body)}"
            else -> exitWithMessage("Syntax: unexpected when branch!", 3)
        }
    }

    private fun forEachStatement(stmt: OrchidNode.ForEachStatement): String {
        val name = stmt.decl.name
        return "var \$l=${expression(stmt.expr)};" +
                "for(var \$i=0,$name=\$l[0];\$i<\$l.length;\$i++,$name=\$l[\$i])" +
                statement(stmt.body)
    }

    private fun expression(expr: OrchidNode.Expression): String {
        return when (expr) {
            is OrchidNode.NumberLiteral -> expr.value.toString()
            is OrchidNode.StringLiteral -> "\"${expr.value}\""
            is OrchidNode.ArrayLiteral -> "[${joinExpr(expr.values)}]"
            is OrchidNode.BoolTrue -> "true"
            is OrchidNode.BoolFalse -> "false"
            is OrchidNode.VarRef -> expr.name.toString()
            is OrchidNode.Exponent -> {
                val leftExpr = expression(expr.left)
                if (expr.assignment)
                    "$leftExpr=Math.pow($leftExpr,${expression(expr.right)})"
                else
                    "Math.pow($leftExpr,${expression(expr.right)})"
            }
            is OrchidNode.ArrayRange -> {
                val left = expression(expr.left)
                val right = expression(expr.right)
                "[...new Array(($right)-($left)).keys()].map(function(x){return x+$left})"
            }
            is OrchidNode.BoolIn -> "${expression(expr.right)}.includes(${expression(expr.left)})"
            is OrchidNode.BinOp -> {
                val left = expression(expr.left)
                val right = expression(expr.right)
                if (expr.assignment) "$left${expr.repr}=$right" else "($left)${expr.repr}($right)"
            }
            is OrchidNode.UnaryOp -> "${expr.repr}${expression(expr.operand)}"
            is OrchidNode.Assignment -> "${expr.name}=${expression(expr.value)}"
            is OrchidNode.FunctionCall -> {
                if (expr.name.parts[0] == "js")
                    "${expr.name.parts.drop(1).joinToString(".")}(${joinExpr(expr.args)})"
                else
                    "${expr.name}(${joinExpr(expr.args)})"
            }
            else -> exitWithMessage("Syntax: expected expression!", 3)
        }
    }

    private fun joinExpr(items: List<OrchidNode.Expression>): String {
        return items.joinToString(",") { expression(it) }
    }
}
