package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token

internal class SelectStatementBuilder(
    baseParamOffset: Int
) : StatementBuilder<ChannelType>(
    baseParamOffset,
    false
) {

    override fun accept(token: Token, index: Int) {}

    override fun create(): Statement =
            SelectStatement(tokens, paramOffsetMap)

    override fun channels(): Array<ChannelType> = emptyArray()
}