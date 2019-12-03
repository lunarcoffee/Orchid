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
                OrchidToken.KVar -> decls += tlVariableDeclaration()
                OrchidToken.KFunc -> TODO("function declaration")
                else -> TODO("statement")
            }
            next = lexer.peek()
        }
        return OrchidNode.Program(runnables, decls)
    }

    private fun tlVariableDeclaration(): OrchidNode.TopLevelVarDecl {
        expectToken<OrchidToken.KVar>()
        val name = expectToken<OrchidToken.ID>().value
        expectToken<OrchidToken.Colon>()
        val type = expectToken<OrchidToken.ID>().value

        return when (lexer.next()) {
            OrchidToken.Terminator -> OrchidNode.TopLevelVarDecl(name, null, type)
            OrchidToken.Equals -> {
                val value = expression()
                expectToken<OrchidToken.Terminator>()
                OrchidNode.TopLevelVarDecl(name, value, type)
            }
            else -> exitWithMessage("Syntax: expected ';' or '='!", 2)
        }
    }

//    private fun <T> functionDeclaration(): OrchidNode.FunctionDefinition<T> {
//
//    }

    private fun expression(): OrchidNode.Expression {
        return when (val next = lexer.next()) {
            is OrchidToken.NumberLiteral -> OrchidNode.NumberLiteral(next.value)
            is OrchidToken.StringLiteral -> OrchidNode.StringLiteral(next.value)
            is OrchidToken.LBracket -> arrayLiteral()
            is OrchidToken.ID -> {
                when (lexer.peek()) {
                    OrchidToken.Terminator -> OrchidNode.VarRef(next.value)
                    OrchidToken.LParen -> functionCall(next.value)
                    else -> exitWithMessage("Syntax: expected ';' or '('!", 2)
                }
            }
            else -> exitWithMessage("Syntax: expected number, string, identifier, or '['!", 2)
        }
    }

    private fun arrayLiteral(): OrchidNode.ArrayLiteral {
        expectToken<OrchidToken.RBracket>()
        val type = expectToken<OrchidToken.ID>().value
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

    // This actually only parses the arguments.
    private fun functionCall(name: String): OrchidNode.FunctionCall {
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
            OrchidToken.KVar::class to "Syntax: expected 'var'!",
            OrchidToken.KFunc::class to "Syntax: expected 'func'!",
            OrchidToken.KReturn::class to "Syntax: expected 'return'!"
        )
    }
}
