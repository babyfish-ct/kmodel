package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.metadata.Column
import org.babyfish.kmodel.jdbc.metadata.Table
import org.babyfish.kmodel.jdbc.metadata.tableManager
import org.babyfish.kmodel.jdbc.sql.AbstractDMLMutationStatement
import java.sql.Connection

abstract class AbstractDMLMutationPlan<S: AbstractDMLMutationStatement>(
    con: Connection,
    protected val statement: S
): ExecutionPlan<DMLMutationResult> {

    protected val table: Table =
        tableManager(con)[con, statement.primaryTable]

    protected val columns: List<Column> by lazy {
        determineColumns()
    }

    protected val imageQuery: ExtraStatement by lazy {
        val prefix = statement
            .primaryTableAlias
            ?.let { "$it." }
            ?: ""
        val selectedColumns = columns.joinToString {
            "$prefix${it.name}"
        }
        ExtraStatementBuilder().apply {
            append("select $selectedColumns from ")
            append(
                statement.tableClauseRange,
                statement
            )
            determineImageQueryCondition()
            append(" for update")
        }.build()
    }

    internal abstract fun determineColumns(): List<Column>

    internal abstract fun ExtraStatementBuilder.determineImageQueryCondition()

    internal fun ExtraStatementBuilder.determineMutationCondition(
        beforeRows: Collection<Row>
    ) {
        addConditionByPkValues(
            table,
            statement.primaryTableAlias,
            beforeRows
        ) { row, _, pkColumn ->
            row[pkColumn.name]!!
        }
    }

    internal open fun mapExtraRow(rsValueGetter: (columnIndex: Int) -> Any?): Row =
        mapRow(table, columns, rsValueGetter)
}