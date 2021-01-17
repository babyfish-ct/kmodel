package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.metadata.Column
import org.babyfish.kmodel.jdbc.metadata.Table
import org.babyfish.kmodel.jdbc.metadata.tableManager
import org.babyfish.kmodel.jdbc.sql.AbstractDMLMutationStatement
import java.lang.IllegalArgumentException
import java.sql.Connection
import java.sql.PreparedStatement

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
        ExtraStatementBuilder().apply {
            append("select ${columns.joinToString(", ") {it.name}} from ")
            append(
                statement.tableSourceRange,
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
            statement.tableAlias,
            beforeRows
        ) { row, _, pkColumn ->
            row[pkColumn.name]!!
        }
    }

    internal open fun mapExtraRow(rsValueGetter: (columnIndex: Int) -> Any?): Row =
        mapRow(table, columns, rsValueGetter)
}