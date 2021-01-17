package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import org.babyfish.kmodel.jdbc.SqlLexer
import java.util.*

abstract class AbstractDMLMutationStatement(
        val fullSql: String,
        tokens: List<Token>,
        paramOffsetMap: NavigableMap<Int, Int>,
        val tableSourceRange: TokenRange,
        val tableAlias: String?
) : Statement(tokens, paramOffsetMap) {

    val primaryTable: String by lazy {
        val builder = StringBuilder()
        var prevToken: Token? = null
        loop@ for (index in tableSourceRange.fromIndex until tableSourceRange.toIndex) {
            val token = tokens[index]
            when (token.type) {
                SqlLexer.IDENTIFIER -> {
                    if (prevToken !== null && prevToken.text != ".") {
                        break@loop
                    }
                    builder.append(token.text)
                }
                SqlLexer.SYMBOL -> {
                    if (token.text != ".") {
                        break@loop
                    }
                    builder.append(".")
                }
                SqlLexer.WS -> {}
                SqlLexer.COMMENT -> {}
                else -> break@loop
            }
            prevToken = token
        }
        builder.toString()
    }
}