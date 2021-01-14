package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import org.babyfish.kmodel.jdbc.SqlLexer
import java.util.*

data class TokenRange(
    val fromIndex: Int,
    val toIndex: Int,
    val paramOffset: Int,
    val paramCount: Int
)

internal fun createTokenRange(
    tokens: List<Token>,
    paramOffsetMap: NavigableMap<Int, Int>,
    fromIndex: Int,
    toIndex: Int = -1
) : TokenRange {

    var startIndex = fromIndex
    for (index in startIndex until tokens.size) {
        val tokenType = tokens[index].type
        if (tokenType != SqlLexer.WS && tokenType != SqlLexer.COMMENT) {
            break
        }
        startIndex++
    }

    var endIndex = if (toIndex == -1) {
        tokens.size
    } else {
        toIndex
    }
    for (index in endIndex downTo 1) {
        val tokenType = tokens[index - 1].type
        if (tokenType != SqlLexer.WS && tokenType != SqlLexer.COMMENT) {
            break
        }
        endIndex--
    }

    if (endIndex < startIndex) {
        endIndex = startIndex
    }

    var paramOffset = paramOffsetMap.floorEntry(startIndex).value
    var paramCount = paramOffsetMap.floorEntry(endIndex).value - paramOffset
    return TokenRange(
        fromIndex = startIndex,
        toIndex = endIndex,
        paramOffset = paramOffset,
        paramCount = paramCount
    )
}

