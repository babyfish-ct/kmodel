package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import org.babyfish.kmodel.jdbc.SqlLexer

internal class DeleteStatementBuilder(
    baseParamOffset: Int,
    private val fullSql: String
) : StatementBuilder<DeleteStatementBuilder.Channel>(
    baseParamOffset,
    true
) {

    private var fromIndex = -1

    private var firstCommaIndex = -1

    private var firstJoinIndex = -1

    private var whereIndex = -1

    override fun accept(token: Token, index: Int) {
        if (depth == 0 && channel == Channel.FROM) {
            if (firstCommaIndex == -1 && token.text == "," ) {
                firstCommaIndex = index
            }
            if (firstJoinIndex == -1 && token.text.equals("join", true)) {
                var joinIndex = index
                preSearchLoop@for (i in index - 2 downTo 3) {
                    if (tokens[i].type == SqlLexer.WS ||
                            tokens[i].type == SqlLexer.COMMENT) {
                        continue
                    }
                    when {
                        tokens[i].text.equals("inner", true) ->
                            joinIndex = i
                        tokens[i].text.equals("left", true) ->
                            joinIndex = i
                        tokens[i].text.equals("right", true) ->
                            joinIndex = i
                        tokens[i].text.equals("full", true) ->
                            joinIndex = i
                        tokens[i].text.equals("outer", true) ->
                            joinIndex = i
                        tokens[i].text.equals("full", true) ->
                            joinIndex = i
                        else -> break@preSearchLoop
                    }
                }
                firstJoinIndex = joinIndex
            }
        }
    }

    override fun create(): Statement {
        val tableEndIndex = positiveIndex(whereIndex)
        val primaryTableEndIndex =
            intArrayOf(
                tableEndIndex,
                positiveIndex(firstCommaIndex),
                positiveIndex(firstJoinIndex)
            ).min()!!
        val tableClauseRange = tokenRange(fromIndex + 1, tableEndIndex)
        val tableRange = tokenRange(fromIndex + 1, primaryTableEndIndex)
        val tableAlias =
            tableAlias(
                tableClauseRange.fromIndex,
                primaryTableEndIndex
            )
        return DeleteStatement(
            fullSql = fullSql,
            tokens = tokens,
            paramOffsetMap = paramOffsetMap,
            tableClauseRange = tableClauseRange,
            tableRange = tableRange,
            tableAlias = tableAlias,
            conditionalRange = if (whereIndex == -1) {
                null
            } else {
                tokenRange(whereIndex + 1)
            }
        )
    }

    override fun channels(): Array<Channel> = enumValues()

    override fun keyword(text: String, index: Int, channelChanged: Boolean) {
        if (channelChanged) {
            when (channel) {
                Channel.FROM -> fromIndex = index
                Channel.WHERE -> whereIndex = index
            }
        }
    }

    enum class Channel(
        keyword: String
    ) : ChannelType {

        FROM("from"),
        WHERE("where");

        override val preDependencyType = PreDependencyType.PREV_ONE

        override val keywords = listOf(keyword)
    }

    private inline fun positiveIndex(index: Int): Int =
        if (index == -1) {
            tokens.size
        } else {
            index
        }
}

