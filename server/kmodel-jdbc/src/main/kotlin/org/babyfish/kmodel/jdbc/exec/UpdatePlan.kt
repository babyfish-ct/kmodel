package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.PreparedStatementProxy
import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.metadata.Column
import org.babyfish.kmodel.jdbc.sql.UpdateStatement
import org.babyfish.kmodel.jdbc.sql.illegalSql
import java.sql.Connection

class UpdatePlan(
    con: Connection,
    statement: UpdateStatement
) : AbstractDMLMutationPlan<UpdateStatement>(
    con, statement
) {
    private val mutationStatementBuilderTemplate =
        ExtraStatementBuilder().apply {
            append("update ")
            append(statement.tableSourceRange, statement)
            append(" set ")
            var addComma = false
            for (updatedAction in statement.updatedActions) {
                append(", ", addComma)
                append(updatedAction.columnRange, statement)
                append(" = ")
                append(updatedAction.valueRange, statement)
                addComma = true
            }
            append(" where ")
            freeze()
        }

    override fun determineColumns(): List<Column> =
        mutableListOf<Column>().apply {
            addAll(table.primaryKeyColumnMap.values)
            addAll(
                statement.updatedActions.mapNotNull {
                    statement.tokens[it.columnRange.toIndex - 1].text.let { col ->
                        if (table.isPrimaryKey(col)) {
                            illegalSql(
                                statement.fullSql,
                                "Cannot update the primary key column",
                                statement.tokens[it.columnRange.fromIndex]
                            )
                        }
                        table.getColumn(col)
                            ?: illegalSql(
                                statement.fullSql,
                                "Unknown updated column \"${statement.tokenRangeText(it.columnRange)}\""
                            )
                    }
                }
            )
        }

    override fun ExtraStatementBuilder.determineImageQueryCondition() {
        append(
            statement.conditionRange,
            statement,
            " where "
        )
    }

    override fun execute(statementProxy: StatementProxy): DMLMutationResult {
        val beforeRowMap = imageQuery.executeQuery(
            statementProxy.targetCon,
            (statementProxy as? PreparedStatementProxy)?.parameterSetters
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
                    (statementProxy as? PreparedStatementProxy)?.parameterSetters
                )
        }
        return DMLMutationResult(
            table = table,
            updateCount = updateCount,
            beforeRowMap = beforeRowMap
        )
    }
}