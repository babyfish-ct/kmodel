package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.ConnectionProxy
import org.babyfish.kmodel.jdbc.DeletionOption
import org.babyfish.kmodel.jdbc.SqlLexer
import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.metadata.Column
import org.babyfish.kmodel.jdbc.metadata.ForeignKey
import org.babyfish.kmodel.jdbc.metadata.tableManager
import org.babyfish.kmodel.jdbc.sql.DeleteStatement
import java.lang.StringBuilder
import java.sql.Connection
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class DeletePlan(
    con: Connection,
    statement: DeleteStatement
) : AbstractDMLMutationPlan<DeleteStatement>(
    con,
    statement
) {

    private val cascadeMapLock = ReentrantReadWriteLock()

    private var _cascadeMap: Map<ForeignKey, Cascade>? = null

    private val tableName: String by lazy {
        val builder = StringBuilder()
        var prevType = SqlLexer.SYMBOL
        statement.tableRange.let {
            for (index in it.fromIndex until it.toIndex) {
                val token = statement.tokens[index]
                if (token.type == SqlLexer.WS || token.type == SqlLexer.COMMENT) {
                    continue
                }
                if (prevType == token.type) {
                    break
                }
                builder.append(token.text)
                prevType = token.type
            }
        }
        builder.toString()
    }

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
            executeCascadeMutations(statementProxy.connection, beforeRowMap)
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

    private fun executeCascadeMutations(
        conProxy: ConnectionProxy,
        beforeRowMap: Map<List<Any>, Row>
    ) {
        if (beforeRowMap.isNotEmpty()) {
            cascadeMap(conProxy.target).values.forEach {
                val behavior = conProxy
                    .cfg
                    .deletionOptionSupplier
                    ?.invoke(it.exportedKey)
                    ?: DeletionOption.NONE
                if (behavior == DeletionOption.SET_NULL) {
                    conProxy.usingSubExecutionContext {
                        it
                            .updateStatementBuilderTemplate
                            .clone()
                            .apply {
                                appendEqualities(
                                    columns = it.exportedKey.childColumns,
                                    rows = beforeRowMap.keys
                                )
                            }
                            .build()
                            .executeUpdate(conProxy, null)
                    }
                } else if (behavior == DeletionOption.CASCADE) {
                    conProxy.usingSubExecutionContext {
                        it
                            .deleteStatementBuilderTemplate
                            .clone()
                            .apply {
                                appendEqualities(
                                    columns = it.exportedKey.childColumns,
                                    rows = beforeRowMap.keys
                                )
                            }
                            .build()
                            .executeUpdate(conProxy, null)
                    }
                }
            }
        }
    }

    private fun cascadeMap(
        con: Connection
    ): Map<ForeignKey, Cascade> =
        cascadeMapLock.read {
            _cascadeMap
        } ?: cascadeMapLock.write {
            _cascadeMap ?:
                   createCascadeMap(con).also {
                       _cascadeMap = it
                   }
        }

    private fun createCascadeMap(
        con: Connection
    ): Map<ForeignKey, Cascade> =
        tableManager(con)[con, tableName]
            .exportedForeignKeys.associateBy({ it }) {
                createCascade(it)
            }

    private fun createCascade(
        foreignKey: ForeignKey
    ): Cascade =
        Cascade(
            exportedKey = foreignKey,
            updateStatementBuilderTemplate =
                ExtraStatementBuilder()
                    .apply {
                        append("update ")
                        append(foreignKey.childTable)
                        append(" set ")
                        var addComma = false
                        for (column in foreignKey.childColumns) {
                            append(", ", addComma)
                            append(column)
                            append(" = null")
                            addComma = true
                        }
                        append(" where ")
                        freeze()
                    },
            deleteStatementBuilderTemplate =
                ExtraStatementBuilder()
                    .apply {
                        append("delete from ")
                        append(foreignKey.childTable)
                        append(" where ")
                        freeze()
                    }
        )

    private class Cascade(
        val exportedKey:ForeignKey,
        val updateStatementBuilderTemplate: ExtraStatementBuilder,
        val deleteStatementBuilderTemplate: ExtraStatementBuilder
    )
}

private fun mapChildKey(
    foreignKey: ForeignKey,
    valueGetter: (Int) -> Any?
): Row {
    val pkColumns = foreignKey.childTable.primaryKeyColumns
    val pkValueMap = pkColumns.indices.associateBy({pkColumns[it].name}) {
        valueGetter(it + 1)!!
    }
    val fkColumns = foreignKey.childColumns
    val fkValueMap = fkColumns.indices.associateBy({fkColumns[it].name}) {
        valueGetter(it + pkColumns.size + 1)
    }
    return Row(pkValueMap, fkValueMap)
}

private fun mapChildRow(
    foreignKey: ForeignKey,
    valueGetter: (Int) -> Any?
): Row {
    val pkColumns = foreignKey.childTable.primaryKeyColumns
    val pkValueMap = pkColumns.indices.associateBy({pkColumns[it].name}) {
        valueGetter(it + 1)!!
    }
    val otherColumns = foreignKey.childTable.columnMap.values - pkColumns
    val otherValueMap = otherColumns.indices.associateBy({otherColumns[it].name}) {
        valueGetter(it + pkColumns.size + 1)
    }
    return Row(pkValueMap, otherValueMap)
}