package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.sql.Statement

class MutationPlan(
    private val statement: Statement
) : ExecutionPlan<Int> {

    override fun execute(
        statementProxy: StatementProxy
    ): Int =
        mutate(statementProxy, statement)
}