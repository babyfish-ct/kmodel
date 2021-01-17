package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.metadata.Column
import org.babyfish.kmodel.jdbc.sql.DeleteStatement
import java.sql.Connection

class DeletePlan(
    con: Connection,
    statement: DeleteStatement
) : AbstractDMLMutationPlan<DeleteStatement>(
    con,
    statement
) {

    private val mutationStatementBuilderTemplate =
        ExtraStatementBuilder().apply {
            append("delete from ")
            append(statement.tableRange, statement)
            append(" where ")
            freeze()
        }

    override fun determineColumns(): List<Column> =
        table.primaryKeyColumnMap.values.toList() +
                table.columnMap.filterKeys {
                    !table.isPrimaryKey(it)
                }.values

    override fun ExtraStatementBuilder.determineImageQueryCondition() {
        append(
            statement.conditionalRange,
            statement,
            " where "
        )
    }

    override fun execute(
        statementProxy: StatementProxy,
        parameters: Parameters?
    ): DMLMutationResult {
        val beforeRowMap = imageQuery.executeQuery(
            statementProxy.targetCon,
            parameters?.setters
        ) {
            mapExtraRow(it)
        }
        val updateCount = if (beforeRowMap.isEmpty()) {
            0
        } else {
            mutationStatementBuilderTemplate
                .clone()
                .apply {
                    determineMutationCondition(beforeRowMap.values)
                }
                .build()
                .executeUpdate(
                    statementProxy.targetCon,
                    parameters?.setters
                )
        }
        return DMLMutationResult(
            table = table,
            updateCount = updateCount,
            beforeRowMap = beforeRowMap
        )
    }
}