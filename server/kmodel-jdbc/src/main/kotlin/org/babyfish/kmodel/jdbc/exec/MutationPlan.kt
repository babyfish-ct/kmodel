package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.sql.Statement

class MutationPlan(
    val statement: Statement
) : ExecutionPlan<Int> {

    override fun execute(
        statementProxy: StatementProxy,
        parameters: Parameters?
    ): Int =
        mutate(statementProxy, statement, parameters)
}