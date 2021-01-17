package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import java.util.*

class UpdateStatement(
    fullSql: String,
    tokens: List<Token>,
    paramOffsetMap: NavigableMap<Int, Int>,
    tableClauseRange: TokenRange,
    tableAlias: String?,
    val updatedActions: List<UpdatedAction>,
    val conditionRange: TokenRange?
) : AbstractDMLMutationStatement(
    fullSql,
    tokens,
    paramOffsetMap,
    tableClauseRange,
    tableClauseRange,
    tableAlias
) {

    private val strVal: String by lazy {
        """{
            |sql = ${tokens.joinToString("") { it.text }},
            |tableClauseRange = $tableClauseRange,
            |updatedActions = $updatedActions,
            |conditionRange = $conditionRange
            |}""".trimMargin()
    }

    override fun toString(): String = strVal
}