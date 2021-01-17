package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import org.babyfish.kmodel.jdbc.SqlLexer
import org.babyfish.kmodel.jdbc.metadata.standardIdentifier
import java.lang.StringBuilder
import java.sql.SQLException

internal class DeleteStatementBuilder(
    baseParamOffset: Int,
    private val fullSql: String
) : StatementBuilder<DeleteStatementBuilder.Channel>(
    baseParamOffset,
    true
) {

    private var fromIndex = -1

    private var whereIndex = -1

    override fun accept(token: Token, index: Int) {

    }

    override fun create(): Statement {
        val endFromIndex = if (whereIndex != -1) {
            whereIndex
        } else {
            tokens.size
        }
        return DeleteStatement(
            fullSql = fullSql,
            tokens = tokens,
            paramOffsetMap = paramOffsetMap,
            tableSourceRange = tokenRange(fromIndex + 1, endFromIndex),
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
}