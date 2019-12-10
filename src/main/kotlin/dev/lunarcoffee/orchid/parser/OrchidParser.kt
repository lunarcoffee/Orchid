package dev.lunarcoffee.orchid.parser

import dev.lunarcoffee.orchid.parser.lexer.Lexer
import dev.lunarcoffee.orchid.parser.lexer.OrchidToken
import dev.lunarcoffee.orchid.util.exitWithMessage

class OrchidParser(override val lexer: Lexer) : Parser {
    override fun getTree() = program()

    private fun program(): OrchidNode.Program {
        var next = lexer.peek()
        val runnables = mutableListOf<OrchidNode.Statement>()
        val decls = mutableListOf<OrchidNode.TopLevelDecl>()

        while (next != OrchidToken.EOF) {
            when (next) {
                OrchidToken.KVar -> runnables += variableDeclaration()
                OrchidToken.KFunc -> decls += functionDeclaration()
                else -> runnables += statement()
            }
            next = lexer.peek()
        }
        return OrchidNode.Program(runnables, decls)
    }

    private fun functionDeclaration(): OrchidNode.FunctionDefinition {
        expectToken<OrchidToken.KFunc>()
        val name = expectToken<OrchidToken.ID>().value
        expectToken<OrchidToken.LParen>()

        var next = lexer.peek()
        val args = mutableMapOf<String, OrchidNode.Type>()

        while (next != OrchidToken.RParen) {
            val paramName = expectToken<OrchidToken.ID>().value
            expectToken<OrchidToken.Colon>()
            args[paramName] = type()

            if (lexer.peek() == OrchidToken.RParen)
                break
            expectToken<OrchidToken.Comma>()
            next = lexer.peek()
        }
        lexer.next()
        expectToken<OrchidToken.Colon>()

        val returnType = type()
        expectToken<OrchidToken.LBrace>()

        next = lexer.peek()
        val body = mutableListOf<OrchidNode.Statement>()
        while (next != OrchidToken.RBrace) {
            body += statement()
            next = lexer.peek()
        }
        lexer.next()

        return OrchidNode.FunctionDefinition(name, args, body, returnType)
    }

    // Parse expressions with precedence climbing.
    private fun expression(minPrecedence: Int = 1): OrchidNode.Expression {
        var left = expressionAtom()
        var next = lexer.peek()

        while (next is OrchidToken.Operator && next.precedence >= minPrecedence) {
            lexer.next()
            val nextPrecedence = next.precedence + if (next.right) 0 else 1
            val right = expression(nextPrecedence)

            left = when (next) {
                OrchidToken.Plus -> OrchidNode.Plus(left, right)
                OrchidToken.Dash -> OrchidNode.Minus(left, right)
                OrchidToken.Asterisk -> OrchidNode.Multiply(left, right)
                OrchidToken.Slash -> OrchidNode.Divide(left, right)
                OrchidToken.Percent -> OrchidNode.Modulo(left, right)
                OrchidToken.Ampersand -> OrchidNode.BitAnd(left, right)
                OrchidToken.Caret -> OrchidNode.BitXor(left, right)
                OrchidToken.Pipe -> OrchidNode.BitOr(left, right)
                OrchidToken.DoubleLAngle -> OrchidNode.BitLShift(left, right)
                OrchidToken.DoubleRAngle -> OrchidNode.BitRShift(left, right)
                OrchidToken.RAnglePipe -> OrchidNode.BitRShiftPad(left, right)
                OrchidToken.DoubleAsterisk -> OrchidNode.Exponent(left, right)
                OrchidToken.DoubleEquals -> OrchidNode.BoolEq(left, right)
                OrchidToken.BangEquals -> OrchidNode.BoolNotEq(left, right)
                OrchidToken.LAngle -> OrchidNode.BoolLess(left, right)
                OrchidToken.RAngle -> OrchidNode.BoolGreater(left, right)
                OrchidToken.LAngleEquals -> OrchidNode.BoolLessEq(left, right)
                OrchidToken.RAngleEquals -> OrchidNode.BoolGreaterEq(left, right)
                OrchidToken.DoubleAmpersand -> OrchidNode.BoolAnd(left, right)
                OrchidToken.DoublePipe -> OrchidNode.BoolOr(left, right)
                OrchidToken.EqualsDot -> OrchidNode.ArrayRange(left, right)
                else -> exitWithMessage("Syntax: unexpected operator!", 2)
            }
            next = lexer.peek()
        }
        return left
    }

    private fun expressionAtom(): OrchidNode.Expression {
        return when (val next = lexer.next()) {
            OrchidToken.LParen -> expression().also { expectToken<OrchidToken.RParen>() }
            is OrchidToken.NumberLiteral -> OrchidNode.NumberLiteral(next.value)
            is OrchidToken.StringLiteral -> OrchidNode.StringLiteral(next.value)
            OrchidToken.KTrue -> OrchidNode.BoolTrue
            OrchidToken.KFalse -> OrchidNode.BoolFalse
            OrchidToken.LBracket -> arrayLiteral()
            OrchidToken.Dash -> OrchidNode.UnaryMinus(expressionAtom())
            OrchidToken.Plus -> OrchidNode.UnaryPlus(expressionAtom())
            OrchidToken.Tilde -> OrchidNode.BitComplement(expressionAtom())
            OrchidToken.Bang -> OrchidNode.BoolNot(expressionAtom())
            is OrchidToken.ID -> {
                val name = scopedName(next.value)
                when (lexer.peek()) {
                    OrchidToken.LParen -> functionCall(name)
                    OrchidToken.Equals -> assignment(name)
                    else -> OrchidNode.VarRef(name)
                }
            }
            else -> exitWithMessage("Syntax: expected number, string, identifier, or '['!", 2)
        }
    }

    // This actually only parses the arguments.
    private fun functionCall(name: OrchidNode.ScopedName): OrchidNode.FunctionCall {
        expectToken<OrchidToken.LParen>()

        var next = lexer.peek()
        val args = mutableListOf<OrchidNode.Expression>()

        while (next != OrchidToken.RParen) {
            args += expression()

            // Support optional trailing comma.
            if (lexer.peek() == OrchidToken.RParen) {
                lexer.next()
                return OrchidNode.FunctionCall(name, args)
            }
            expectToken<OrchidToken.Comma>()
            next = lexer.peek()
        }

        expectToken<OrchidToken.RParen>()
        return OrchidNode.FunctionCall(name, args)
    }

    private fun assignment(name: OrchidNode.ScopedName): OrchidNode.Assignment {
        expectToken<OrchidToken.Equals>()
        return OrchidNode.Assignment(name, expression())
    }

    private fun statement(): OrchidNode.Statement {
        return when (lexer.peek()) {
            is OrchidToken.KVar -> variableDeclaration()
            is OrchidToken.KReturn -> returnStatement()
            is OrchidToken.KIf -> ifStatement()
            is OrchidToken.KWhen -> whenStatement()
            is OrchidToken.LBrace -> scope()
            else -> expression().also { expectToken<OrchidToken.Terminator>() }
        }
    }

    private fun variableDeclaration(): OrchidNode.VarDecl {
        expectToken<OrchidToken.KVar>()
        val name = expectToken<OrchidToken.ID>().value
        expectToken<OrchidToken.Colon>()
        val type = type()

        return when (lexer.next()) {
            OrchidToken.Terminator -> OrchidNode.VarDecl(name, null, type)
            OrchidToken.Equals -> {
                val value = expression()
                expectToken<OrchidToken.Terminator>()
                OrchidNode.VarDecl(name, value, type)
            }
            else -> exitWithMessage("Syntax: expected ';' or '='!", 2)
        }
    }

    private fun returnStatement(): OrchidNode.Return {
        expectToken<OrchidToken.KReturn>()
        return OrchidNode.Return(expression()).also { expectToken<OrchidToken.Terminator>() }
    }

    private fun ifStatement(): OrchidNode.IfStatement {
        expectToken<OrchidToken.KIf>()

        expectToken<OrchidToken.LParen>()
        val condition = expression()
        expectToken<OrchidToken.RParen>()

        val body = statement()
        val elseStmt = if (lexer.peek() == OrchidToken.KElse) {
            lexer.next()
            statement()
        } else {
            null
        }

        return OrchidNode.IfStatement(condition, body, elseStmt)
    }

    private fun whenStatement(): OrchidNode.WhenStatement {
        expectToken<OrchidToken.KWhen>()

        expectToken<OrchidToken.LParen>()
        val cmpExpr = expression()
        expectToken<OrchidToken.RParen>()

        expectToken<OrchidToken.LBrace>()

        val branches = mutableListOf<OrchidNode.WhenBranch>()
        var next = lexer.peek()
        while (next != OrchidToken.RBrace) {
            branches += whenBranch()
            next = lexer.peek()
        }

        lexer.next()
        return OrchidNode.WhenStatement(cmpExpr, branches)
    }

    private fun whenBranch(): OrchidNode.WhenBranch {
        return when (lexer.peek()) {
            is OrchidToken.KElse -> {
                lexer.next()
                expectToken<OrchidToken.RArrow>()
                OrchidNode.WhenElseBranch(statement())
            }
            is OrchidToken.KIn -> {
                lexer.next()
                val arrExpr = expression()
                expectToken<OrchidToken.RArrow>()
                OrchidNode.WhenInBranch(arrExpr, statement())
            }
            // Assume equality check branch.
            else -> {
                val exprs = mutableListOf(expression())
                var next = lexer.peek()
                while (next == OrchidToken.Comma) {
                    lexer.next()
                    exprs += expression()
                    next = lexer.peek()
                }

                expectToken<OrchidToken.RArrow>()
                OrchidNode.WhenEqBranch(exprs, statement())
            }
        }
    }

    private fun scope(): OrchidNode.Scope {
        expectToken<OrchidToken.LBrace>()

        val body = mutableListOf<OrchidNode.Statement>()
        var next = lexer.peek()

        while (next != OrchidToken.RBrace) {
            body += statement()
            next = lexer.peek()
        }

        expectToken<OrchidToken.RBrace>()
        return OrchidNode.Scope(body)
    }

    private fun arrayLiteral(): OrchidNode.ArrayLiteral {
        expectToken<OrchidToken.RBracket>()
        val type = type()
        expectToken<OrchidToken.LBrace>()

        var next = lexer.peek()
        val values = mutableListOf<OrchidNode.Expression>()

        while (next != OrchidToken.RBrace) {
            values += expression()

            // Support optional trailing comma.
            if (lexer.peek() == OrchidToken.RBrace) {
                lexer.next()
                return OrchidNode.ArrayLiteral(values, type)
            }
            expectToken<OrchidToken.Comma>()
            next = lexer.peek()
        }

        expectToken<OrchidToken.RBrace>()
        return OrchidNode.ArrayLiteral(values, type)
    }

    private fun type(): OrchidNode.Type {
        val baseType = scopedName(expectToken<OrchidToken.ID>().value)
        val typeParams = mutableListOf<OrchidNode.Type>()
        var next = lexer.peek()

        if (lexer.peek() is OrchidToken.LAngle) {
            lexer.next()
            while (next !is OrchidToken.RAngle) {
                typeParams += type()

                // Support optional trailing comma.
                if (lexer.peek() == OrchidToken.RAngle) {
                    lexer.next()
                    return OrchidNode.Type(baseType, true, typeParams)
                }
                expectToken<OrchidToken.Comma>()
                next = lexer.peek()
            }
            expectToken<OrchidToken.RAngle>()
        }

        return OrchidNode.Type(baseType, typeParams.isNotEmpty(), typeParams.ifEmpty { null })
    }

    // This actually only parses the parts (if they exist) after the first section [base].
    private fun scopedName(base: String): OrchidNode.ScopedName {
        if (lexer.peek() != OrchidToken.Dot)
            return OrchidNode.ScopedName(listOf(base))

        val parts = mutableListOf(base)
        var next = lexer.peek()
        while (next == OrchidToken.Dot) {
            lexer.next()
            parts += expectToken<OrchidToken.ID>().value
            next = lexer.peek()
        }
        return OrchidNode.ScopedName(parts)
    }

    // Try to get a token, exiting if the next token in the stream is not the type expected.
    private inline fun <reified T : OrchidToken> expectToken(errorMessage: String? = null): T {
        val token = lexer.next()
        if (token !is T) {
            val message = errorMessage ?: expectedMessages[T::class] ?: error("Fatal: unknown!")
            exitWithMessage(message, 2)
        }
        return token
    }

    companion object {
        private val expectedMessages = mapOf(
            OrchidToken.ID::class to "Syntax: expected identifier!",
            OrchidToken.NumberLiteral::class to "Syntax: expected number literal!",
            OrchidToken.StringLiteral::class to "Syntax: expected string literal!",
            OrchidToken.Colon::class to "Syntax: expected ':'!",
            OrchidToken.Dot::class to "Syntax: expected '.'!",
            OrchidToken.Comma::class to "Syntax: expected ','!",
            OrchidToken.Equals::class to "Syntax: expected '='!",
            OrchidToken.Terminator::class to "Syntax: expected ';'!",
            OrchidToken.LBrace::class to "Syntax: expected '{'!",
            OrchidToken.RBrace::class to "Syntax: expected '}'!",
            OrchidToken.LParen::class to "Syntax: expected '('!",
            OrchidToken.RParen::class to "Syntax: expected ')'!",
            OrchidToken.LBracket::class to "Syntax: expected '['!",
            OrchidToken.RBracket::class to "Syntax: expected ']'!",
            OrchidToken.LAngle::class to "Syntax: expected '<'!",
            OrchidToken.RAngle::class to "Syntax: expected '>'!",
            OrchidToken.EOF::class to "Syntax: expected the end of file!",
            OrchidToken.Plus::class to "Syntax: expected '+'!",
            OrchidToken.Dash::class to "Syntax: expected '-'!",
            OrchidToken.Asterisk::class to "Syntax: expected '*'!",
            OrchidToken.Slash::class to "Syntax: expected '/'!",
            OrchidToken.Percent::class to "Syntax: expected '%'!",
            OrchidToken.DoubleAsterisk::class to "Syntax: expected '**'!",
            OrchidToken.Ampersand::class to "Syntax: expected '&'!",
            OrchidToken.Caret::class to "Syntax: expected '^'!",
            OrchidToken.Pipe::class to "Syntax: expected '|'!",
            OrchidToken.DoubleEquals::class to "Syntax: expected '=='!",
            OrchidToken.BangEquals::class to "Syntax: expected '!='!",
            OrchidToken.LAngleEquals::class to "Syntax: expected '<='!",
            OrchidToken.RAngleEquals::class to "Syntax: expected '>='!",
            OrchidToken.DoubleAmpersand::class to "Syntax: expected '&&'!",
            OrchidToken.DoublePipe::class to "Syntax: expected '||'!",
            OrchidToken.Tilde::class to "Syntax: expected '~'!",
            OrchidToken.Bang::class to "Syntax: expected '!'!",
            OrchidToken.KVar::class to "Syntax: expected 'var'!",
            OrchidToken.KFunc::class to "Syntax: expected 'func'!",
            OrchidToken.KReturn::class to "Syntax: expected 'return'!",
            OrchidToken.KIf::class to "Syntax: expected 'if'!",
            OrchidToken.KElse::class to "Syntax: expected 'else'!",
            OrchidToken.KTrue::class to "Syntax: expected 'true'!",
            OrchidToken.KFalse::class to "Syntax: expected 'false'!"
        )
    }
}
