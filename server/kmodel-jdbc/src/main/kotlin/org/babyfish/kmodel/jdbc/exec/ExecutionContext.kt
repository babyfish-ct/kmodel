package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.ConnectionProxy
import org.babyfish.kmodel.jdbc.DataChangedEvent
import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.metadata.QualifiedName
import org.babyfish.kmodel.jdbc.metadata.Table
import org.babyfish.kmodel.jdbc.metadata.tableManager
import org.babyfish.kmodel.jdbc.sql.parseSqlStatements
import java.sql.ResultSet
import java.sql.SQLException

internal class ExecutionContext(
    private val dataChangedListener: (DataChangedEvent) -> Unit
) {

    private var statementProxy: StatementProxy? = null

    private var plans: List<ExecutionPlan<*>>? = null

    private var planIndex = 0

    private val beforeImageMap =
        mutableMapOf<
                QualifiedName, //tableName
                MutableMap<List<Any>, Row?> // oldRows
        >()

    fun execute(
            sql: String,
            statementProxy: StatementProxy
    ): Boolean {
        this.statementProxy = statementProxy
        plans = statementProxy.targetCon.executionPlans(sql)
        planIndex = 0
        return if (plans === null) {
            statementProxy.target.execute(sql)
        } else {
            isResultSet()
        }
    }

    fun execute(
        sqls: List<String>,
        statementProxy: StatementProxy
    ): IntArray =
        when {
            sqls.isEmpty() ->
                intArrayOf()
            sqls.size == 1 -> {
                execute(sqls[0], statementProxy)
                intArrayOf(
                    getUpdateCount()
                )
            } else -> {
                this.statementProxy = statementProxy
                plans = sqls.flatMap {
                    statementProxy.targetCon.executionPlans(it)
                        ?: parseSqlStatements(it) // parsed result has cache, won't really parse again
                            .map {  stmt ->
                                MutationPlan(stmt)
                            }
                }
                planIndex = 0
                val updateCounts = mutableListOf<Int>()
                while (true) {
                    val updatedCount = getUpdateCount()
                    if (updatedCount == -1) {
                        break
                    }
                    updateCounts += updatedCount
                    getMoreResults()
                }
                updateCounts.toIntArray()
            }
        }

    fun getMoreResults(): Boolean {
        val stmtProxy = statementProxy ?: noExecutedStatement()
        return if (plans == null) {
            stmtProxy.target.moreResults
        } else {
            planIndex++
            isResultSet()
        }
    }

    fun getResultSet(): ResultSet? {
        val stmtProxy = statementProxy ?: noExecutedStatement()
        val plans = this.plans
        return if (plans == null) {
            stmtProxy.target.resultSet
        } else {
            return if (planIndex >= plans.size || plans[planIndex] !is SelectPlan) {
                null
            } else {
                plans[planIndex].let {
                    (it as SelectPlan).execute(stmtProxy)
                }
            }
        }
    }

    fun getUpdateCount(): Int {
        val stmtProxy = statementProxy ?: noExecutedStatement()
        val plans = this.plans
        return if (plans == null) {
            stmtProxy.target.updateCount
        } else {
            return if (planIndex >= plans.size || plans[planIndex] is SelectPlan) {
                -1
            } else {
                plans[planIndex].let {
                    val result = it.execute(stmtProxy)
                    if (result is DMLMutationResult) {
                        beforeImageMap
                            .computeIfAbsent(result.table.qualifiedName) {
                                mutableMapOf()
                            }
                            .apply {
                                for ((pkValues, row) in result.beforeRowMap) {
                                    putIfAbsent(pkValues, row)
                                }
                            }
                        result.updateCount
                    } else {
                        result as Int
                    }
                }
            }
        }
    }

    fun commit(conProxy: ConnectionProxy) {
        val tableMap = mutableMapOf<QualifiedName, Table>()
        val afterImageMap = mutableMapOf<
                QualifiedName, //tableName
                Map<List<Any>, Row>
        >()

        val tableManager = tableManager(conProxy.target)
        for ((qualifiedName) in beforeImageMap) {
            val table = tableManager[conProxy.target, qualifiedName.toString()]
            tableMap[qualifiedName] = table
            afterImageMap[qualifiedName] = afterRowMap(table, conProxy)
        }
        dataChangedListener(
            DataChangedEvent(
                beforeImageMap = beforeImageMap,
                afterImageMap = afterImageMap
            )
        )
    }

    private fun isResultSet(): Boolean {
        val plans = this.plans ?: error("Internal bug")
        return if (planIndex < plans.size) {
            plans[planIndex] is SelectPlan
        } else {
            false
        }
    }

    private fun noExecutedStatement(): Nothing {
        throw SQLException("No executed statement")
    }

    private fun afterRowMap(
        table: Table,
        conProxy: ConnectionProxy
    ) : Map<List<Any>, Row> {
        val beforeRowMap = beforeImageMap[table.qualifiedName]
            ?: return emptyMap()
        return ExtraStatementBuilder()
            .apply {
                append("select ")
                var addComma = false
                for (column in table.columnMap.values) {
                    append(", ", addComma)
                    append(column)
                    addComma = true
                }
                append(" from ")
                append(table)
                append(" where ")
                addConditionByPkValues(
                    table,
                    beforeImageMap[table.qualifiedName]!!.keys
                ) { row, pkColumnIndex, _ ->
                    row[pkColumnIndex]
                }
            }
            .build()
            .executeQuery(
                conProxy.target,
                emptyList()
            ) {
                mapRow(
                    table,
                    table.columnMap.values.toList(),
                    it
                )
            }
    }
}