package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import org.babyfish.kmodel.jdbc.SqlLexer

data class UpdatedAction(
    val columnRange: TokenRange,
    val valueRange: TokenRange
)

internal class UpdatedActionListBuilder(
        private val statementBuilder: StatementBuilder<*>
) {
    private var columnIndex = -1

    private var assignIndex = -1

    private var columnRange: TokenRange? = null

    private var lastIndex = -1

    private val updatedActions = mutableListOf<UpdatedAction>()

    fun append(token: Token, index: Int, depth: Int) {
        if (columnIndex == -1) {
            if (token.type == SqlLexer.WS ||
                    token.type == SqlLexer.COMMENT ||
                    token.text.equals("set", true)) {
                return
            }
            columnIndex = index
        }
        if (depth == 0) {
            when (token.text) {
                "=" -> {
                    columnRange = statementBuilder.tokenRange(columnIndex, index)
                    assignIndex = index
                }
                "," -> {
                    submit(index)
                }
            }
        }
        lastIndex = index
    }

    fun build(): List<UpdatedAction> {
        submit(lastIndex + 1)
        return updatedActions
    }

    private fun submit(index: Int) {
        if (columnRange !== null && assignIndex != -1) {
            val valueRange = statementBuilder
                    .tokenRange(
                            assignIndex + 1,
                            index
                    )
            updatedActions += UpdatedAction(
                    columnRange = columnRange!!,
                    valueRange = valueRange
            )
            columnIndex = -1
            columnRange = null
            assignIndex = -1
        }
    }
}
