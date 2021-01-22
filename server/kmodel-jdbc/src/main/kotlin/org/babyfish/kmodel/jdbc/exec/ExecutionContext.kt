package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.*
import org.babyfish.kmodel.jdbc.metadata.ForeignKey
import org.babyfish.kmodel.jdbc.metadata.QualifiedName
import org.babyfish.kmodel.jdbc.metadata.Table
import org.babyfish.kmodel.jdbc.metadata.tableManager
import org.babyfish.kmodel.jdbc.sql.AbstractDMLMutationStatement
import org.babyfish.kmodel.jdbc.sql.DdlStatement
import org.babyfish.kmodel.jdbc.sql.illegalSql
import org.babyfish.kmodel.jdbc.sql.parseSqlStatements
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

internal open class ExecutionContext protected constructor(
    protected val cfg: Configuration,
    protected val beforeImageMap: MutableMap<
            QualifiedName,
            MutableMap<List<Any>, Row?>
    >
) {
    private var statementProxy: StatementProxy? = null

    private var executions: List<Execution>? = null

    private var index = 0

    fun subContext(): ExecutionContext =
        ExecutionContext(cfg, beforeImageMap)

    fun execute(
        sql: String,
        parameters: Parameters?,
        statementProxy: StatementProxy
    ): Boolean {
        this.statementProxy = statementProxy
        executions = statementProxy
            .targetCon
            .executionPlans(sql)
            ?.map {
                Execution(
                    batchIndex = 0,
                    plan = it,
                    parameters = parameters
                )
            }

        index = 0
        return if (executions === null) {
            if (parameters === null) {
                statementProxy.target.execute(sql)
            } else {
                val targetPreparedStatement =
                    statementProxy.target as PreparedStatement
                for (parameterIndex in 1 until parameters.setters.size) {
                    val setter = parameters.setters[parameterIndex]
                    if (setter !== null) {
                        targetPreparedStatement
                            .setter(parameterIndex)
                    }
                }
                targetPreparedStatement.execute()
            }
        } else {
            isResultSet()
        }
    }

    fun execute(
        batches: List<Batch>,
        statementProxy: StatementProxy
    ): IntArray =
        if (batches.isEmpty()) {
            intArrayOf()
        } else {
            var hasDdl = false
            this.statementProxy = statementProxy
            var batchIndex = 0
            executions = batches.flatMap {
                val newStatements = parseSqlStatements(it.sql)
                if (hasDdl && newStatements.any { stmt -> stmt is AbstractDMLMutationStatement }) {
                    illegalSql(it.sql, "Cannot mix DDL and DML mutation in one statement batch")
                }
                val newPlans = statementProxy.targetCon.executionPlans(it.sql)
                    ?: newStatements
                        .map {  stmt ->
                            MutationPlan(stmt)
                        }
                hasDdl = hasDdl || newPlans.any { plan ->
                    plan is MutationPlan && plan.statement is DdlStatement
                }
                newPlans.map { plan ->
                    Execution(
                        batchIndex = batchIndex,
                        plan = plan,
                        parameters = it.parameters
                    )
                }.also {
                    batchIndex++
                }
            }
            index = 0
            val updateCounts = mutableListOf<Int>()
            while (true) {
                val updateCount = getUpdateCount()
                if (updateCount == -1) {
                    break
                }
                val batchIndex = executions!![index].batchIndex
                if (batchIndex < updateCounts.size) {
                    updateCounts[batchIndex] += updateCount
                } else {
                    updateCounts.add(batchIndex, updateCount)
                }
                getMoreResults()
            }
            updateCounts.toIntArray()
        }

    fun getMoreResults(): Boolean {
        val stmtProxy = statementProxy ?: noExecutedStatement()
        return if (executions == null) {
            stmtProxy.target.moreResults
        } else {
            index++
            isResultSet()
        }
    }

    fun getResultSet(): ResultSet? {
        val stmtProxy = statementProxy ?: noExecutedStatement()
        val executions = this.executions
        return if (executions == null) {
            stmtProxy.target.resultSet
        } else {
            return if (index >= executions.size || executions[index].plan !is SelectPlan) {
                null
            } else {
                executions[index].let {
                    (it.plan as SelectPlan).execute(
                        stmtProxy,
                        it.parameters
                    )
                }
            }
        }
    }

    fun getUpdateCount(): Int {
        val stmtProxy = statementProxy ?: noExecutedStatement()
        val executions = this.executions
        return if (executions == null) {
            stmtProxy.target.updateCount
        } else {
            return if (index >= executions.size || executions[index].plan is SelectPlan) {
                -1
            } else {
                executions[index].let {
                    val result = it.plan.execute(stmtProxy, it.parameters)
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

    private fun isResultSet(): Boolean {
        val executions = this.executions ?: error("Internal bug")
        return if (index < executions.size) {
            executions[index].plan is SelectPlan
        } else {
            false
        }
    }

    private fun noExecutedStatement(): Nothing {
        throw SQLException("No executed statement")
    }

    private data class Execution(
        val batchIndex: Int,
        val plan: ExecutionPlan<*>,
        val parameters: Parameters?
    )
}