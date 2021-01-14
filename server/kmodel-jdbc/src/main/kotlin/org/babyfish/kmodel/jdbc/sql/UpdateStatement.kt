package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import java.util.*

class UpdateStatement(
    fullSql: String,
    tokens: List<Token>,
    paramOffsetMap: NavigableMap<Int, Int>,
    tableSourceRange: TokenRange,
    val updatedActions: List<UpdatedAction>,
    val conditionRange: TokenRange?
) : AbstractDMLMutationStatement(
    fullSql,
    tokens,
    paramOffsetMap,
    tableSourceRange
) {

    private val strVal: String by lazy {
        """{
            |sql = ${tokens.joinToString("") { it.text }},
            |tableSourceRange = $tableSourceRange,
            |updatedActions = $updatedActions,
            |conditionRange = $conditionRange
            |}""".trimMargin()
    }

    override fun toString(): String = strVal
}