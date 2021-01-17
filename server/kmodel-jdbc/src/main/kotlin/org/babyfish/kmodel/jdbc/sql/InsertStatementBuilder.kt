package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import org.babyfish.kmodel.jdbc.SqlLexer

internal class InsertStatementBuilder(
    baseParamOffset: Int,
    private val fullSql: String
): StatementBuilder<InsertStatementBuilder.Channel>(
    baseParamOffset,
    true
) {

    private var tableFromIndex = -1

    private var tableToIndex = -1

    private var constraintIndex = -1

    private val insertedColumnRanges = mutableListOf<TokenRange>()

    private var rowBuilder: RowBuilder? = null

    private val rows = mutableListOf<InsertStatement.Row>()

    private val conflictColumnNames = mutableListOf<String>()

    private var conflictConstraintName: String? = null

    private var updatedActionListBuilder: UpdatedActionListBuilder? = null

    override fun accept(token: Token, index: Int) {
        if (channel == Channel.TABLE_NAME) {
            if (depth == 0) {
                if (token.text.equals("from", true) ||
                        token.text.equals("join", true) ||
                        token.text.equals("select", true)) {
                    illegalSql(fullSql, "'${token.text}' is not supported'", token)
                }
                if (token.type != SqlLexer.WS &&
                    token.type != SqlLexer.COMMENT &&
                    token.text != "(" &&
                    token.text != ")"
                ) {
                    if (tableFromIndex == -1) {
                        tableFromIndex = index
                    }
                    tableToIndex = index
                }
            } else if (depth == 1 && token.type == SqlLexer.IDENTIFIER) {
                insertedColumnRanges += tokenRange(index, index + 1)
            }
        } else if (channel == Channel.VALUES) {
            if (depth == 1 && token.text == "(") {
                rowBuilder = RowBuilder(this)
            } else if (depth == 0 && token.text == ")") {
                rowBuilder?.build()?.let {
                    rows += it
                }
                rowBuilder = null
            } else {
                rowBuilder?.append(token, index)
            }
        } else if (channel == Channel.CONFLICT) {
            if (updatedActionListBuilder === null && token.type == SqlLexer.IDENTIFIER) {
                if (constraintIndex != -1) {
                    conflictConstraintName = token.text
                } else if (depth == 1) {
                    conflictColumnNames += token.text
                }
            } else {
                updatedActionListBuilder?.append(token, index, depth)
            }
        }
    }

    override fun create(): Statement {
        if (rows.isEmpty()) {
            illegalSql(fullSql,"No inserted rows is specified by 'values', inserted by sub query is not supported")
        }
        val tableSourceRange = tokenRange(tableFromIndex, tableToIndex + 1)
        val updatedActions = updatedActionListBuilder?.build() ?: emptyList()
        val conflictPolicy =
            if (channel!! >= Channel.CONFLICT) {
                InsertStatement.ConflictPolicy(
                        columnNames = conflictColumnNames,
                        constraintName = conflictConstraintName,
                        updatedActions = updatedActions
                )
            } else {
                null
            }
        return InsertStatement(
            fullSql = fullSql,
            tokens = tokens,
            paramOffsetMap = paramOffsetMap,
            tableSourceRange = tableSourceRange,
            insertedColumnRanges = insertedColumnRanges,
            rows = rows,
            conflictPolicy = conflictPolicy
        )
    }

    override fun channels(): Array<Channel> =
            enumValues()

    override fun keyword(text: String, index: Int, channelChanged: Boolean) {
        when (text) {
            "constraint" -> constraintIndex = index
            "update" -> updatedActionListBuilder =
                    UpdatedActionListBuilder(this)
        }
    }

    enum class Channel(
            vararg keywords: String
    ) : ChannelType {

        TABLE_NAME("into"),
        VALUES("values"),
        CONFLICT("on", "conflict", "constraint", "duplicate", "key", "do", "update", "set", "nothing");

        override val preDependencyType = PreDependencyType.PREV_ALL

        override val keywords = keywords.toList()
    }
}

private class RowBuilder(
        private val statementBuilder: InsertStatementBuilder
) {

    private val values = mutableListOf<TokenRange>()

    private var firstIndex = -1

    private var lastIndex = -1

    fun append(token: Token, index: Int) {
        if (firstIndex == -1) {
            firstIndex = index
        }
        if (token.text == ",") {
            submit(index)
        }
        lastIndex = index
    }

    fun build(): InsertStatement.Row {
        submit(lastIndex + 1)
        return InsertStatement.Row(values)
    }

    private fun submit(index: Int) {
        if (firstIndex != -1) {
            values += statementBuilder.tokenRange(firstIndex, index)
            firstIndex = -1
        }
    }
}
