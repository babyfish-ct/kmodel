package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import java.util.*

class DeleteStatement(
    fullSql: String,
    tokens: List<Token>,
    paramOffsetMap: NavigableMap<Int, Int>,
    tableClauseRange: TokenRange,
    tableRange: TokenRange,
    tableAlias: String?,
    val conditionalRange: TokenRange?
): AbstractDMLMutationStatement(
    fullSql,
    tokens,
    paramOffsetMap,
    tableClauseRange,
    tableRange,
    tableAlias
) {

    private val strVal: String by lazy {
        """{
            |sql = ${tokens.joinToString("") { it.text }},
            |tableClauseRange = $tableClauseRange,
            |conditionalRange = $conditionalRange
            |}""".trimMargin()
    }

    override fun toString(): String = strVal
}
