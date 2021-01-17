package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import java.util.*

class InsertStatement(
    fullSql: String,
    tokens: List<Token>,
    paramOffsetMap: NavigableMap<Int, Int>,
    tableClauseRange: TokenRange,
    val insertedColumnRanges: List<TokenRange>,
    val rows: List<Row>,
    val conflictPolicy: ConflictPolicy?
): AbstractDMLMutationStatement(
    fullSql,
    tokens,
    paramOffsetMap,
    tableClauseRange,
    tableClauseRange,
    null
) {

    private val strVal: String by lazy {
        """{
            |sql = ${tokens.joinToString("") { it.text }},
            |tableClauseRange = $tableClauseRange,
            |insertedColumns = $insertedColumnRanges,
            |rows = $rows
            |conflictPolicy = $conflictPolicy
            |}""".trimMargin()
    }

    override fun toString(): String = strVal

    data class Row(
            val values: List<TokenRange>
    )

    data class ConflictPolicy(
            val columnNames: List<String>,
            val constraintName: String?,
            val updatedActions: List<UpdatedAction>
    )
}