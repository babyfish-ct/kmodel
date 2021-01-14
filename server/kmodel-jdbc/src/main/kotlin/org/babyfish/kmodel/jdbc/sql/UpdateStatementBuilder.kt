package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token

internal class UpdateStatementBuilder(
        private val fullSql: String
) : StatementBuilder<UpdateStatementBuilder.Channel>(true) {

    private var setIndex = -1

    private var whereIndex = -1

    private var updatedActionListBuilder: UpdatedActionListBuilder? = null

    override fun accept(token: Token, index: Int) {
        when (channel) {
            null -> {
                if (depth == 0 && (
                                token.text == "," ||
                                        token.text.equals("from", true) ||
                                        token.text.equals("join", true))) {
                    illegalSql(fullSql, "Unexpected token ${token.text}", token)
                }
            }
            Channel.SET -> {
                if (depth == 0 && (
                                token.text.equals("from", true) ||
                                        token.text.equals("join", true))) {
                    illegalSql(fullSql, "Unexpected token ${token.text}", token)
                }
                updatedActionListBuilder?.append(token, index, depth)
            }
        }
    }

    override fun create(): Statement {
        return UpdateStatement(
            fullSql = fullSql,
            tokens = tokens,
            paramOffsetMap = paramOffsetMap,
            tableSourceRange = tokenRange(1, setIndex),
            updatedActions = updatedActionListBuilder?.build() ?: emptyList(),
            conditionRange = if (whereIndex == -1) {
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
                Channel.SET -> {
                    updatedActionListBuilder = UpdatedActionListBuilder(this)
                    setIndex = index
                }
                Channel.WHERE -> {
                    whereIndex = index
                }
            }
        }
    }

    enum class Channel(
            keyword: String
    ) : ChannelType {

        SET("set"),
        WHERE("where");

        override val preDependencyType = PreDependencyType.PREV_ONE

        override val keywords: List<String> = listOf(keyword)
    }
}