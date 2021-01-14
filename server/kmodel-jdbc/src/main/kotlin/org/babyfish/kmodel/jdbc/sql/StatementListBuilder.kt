package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import org.babyfish.kmodel.jdbc.SqlLexer

class StatementListBuilder(
        private val sql: String
) {

    private var statementBuilder: StatementBuilder<*>? = null

    private val statements = mutableListOf<Statement>()

    fun append(token: Token) {
        if (token.type == SqlLexer.EOF || token.text == ";") {
            submit()
        } else {
            if (token.type != SqlLexer.WS &&
                    token.type != SqlLexer.COMMENT &&
                    statementBuilder === null
            ) {
                statementBuilder = statementBuilder(token)
            }
            statementBuilder?.append(token)
        }
    }

    fun build(): List<Statement> {
        submit()
        return statements
    }

    private fun submit() {
        statementBuilder?.build()?.also {
            statements += it
        }
        statementBuilder = null
    }

    private fun statementBuilder(firstToken: Token): StatementBuilder<*> =
            when (firstToken.text.toLowerCase()) {
                "insert" -> InsertStatementBuilder(sql)
                "update" -> UpdateStatementBuilder(sql)
                "delete" -> DeleteStatementBuilder(sql)
                "select" -> SelectStatementBuilder()
                else -> DdlStatementBuilder()
            }
}