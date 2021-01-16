package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.sql.*
import org.babyfish.kmodel.jdbc.sql.parseSqlStatements
import java.sql.Connection
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

interface ExecutionPlan<R> {

    fun execute(
        statementProxy: StatementProxy
    ): R
}

fun Connection.executionPlans(
    sql: String
): List<ExecutionPlan<*>>? {
    val key = PlanKey(metaData.url, metaData.userName, sql)
    val planList = planListLock.read {
        planListMap[key]
    } ?: planListLock.write {
        planListMap[key] ?:
                createExecutionPlans(this, sql).also {
                    planListMap[key] = it
                }
    }
    return planList.takeIf {
        it.isNotEmpty()
    }
}

private val planListLock = ReentrantReadWriteLock()

private val planListMap =
    object: LinkedHashMap<PlanKey, List<ExecutionPlan<*>>>(
        512,
        .75F,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<PlanKey, List<ExecutionPlan<*>>>?
        ): Boolean = true
    }

private fun createExecutionPlans(
    con: Connection,
    sql: String
): List<ExecutionPlan<*>> {
    val sqlStatements = parseSqlStatements(sql)
    for (i in sqlStatements.indices) {
        for (ii in i + 1 until sqlStatements.size) {
            if (!areCompatible(sqlStatements[i], sqlStatements[ii])) {
                illegalSql(sql, "Cannot mix DDL and DML mutation in one statement")
            }
        }
    }
    val requireInterceptor = sqlStatements.any {
        it is InsertStatement || it is UpdateStatement || it is DeleteStatement
    }
    return if (requireInterceptor) {
        sqlStatements.map {
            createExecutionPlan(con, it)
        }
    } else {
        emptyList()
    }
}

private fun createExecutionPlan(
    con: Connection,
    statement: Statement
) : ExecutionPlan<*> =
    when (statement) {
        is InsertStatement -> InsertPlan(con, statement)
        is UpdateStatement -> UpdatePlan(con, statement)
        is DeleteStatement -> DeletePlan(con, statement)
        is SelectStatement -> SelectPlan(statement)
        else -> MutationPlan(statement)
    }

private data class PlanKey(
    val connectionString: String,
    val userName: String,
    val sql: String
)

private inline fun areCompatible(
    statement1: Statement,
    statement2: Statement
): Boolean =
    when {
        statement1 is AbstractDMLMutationStatement &&
                statement2 is DdlStatement ->
            false
        statement1 is DdlStatement &&
                statement2 is AbstractDMLMutationStatement ->
            false
        else ->
            true
    }