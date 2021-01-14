package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.PreparedStatementProxy
import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.sql.SelectStatement
import java.sql.ResultSet

class SelectPlan(
    private val statement: SelectStatement
) : ExecutionPlan<ResultSet> {

    override fun execute(
        statementProxy: StatementProxy
    ): ResultSet =
        if (statementProxy is PreparedStatementProxy) {
            statementProxy
                .targetCon
                .prepareStatement(statement.sql)
                .using {
                    for (parameterIndex in 1 until statementProxy.parameterSetters.size) {
                        statementProxy
                            .parameterSetters[parameterIndex]
                            ?.let { setter ->
                                it.setter(parameterIndex)
                            }
                    }
                    it.executeQuery()
                }
        } else {
            statementProxy
                .targetCon
                .createStatement()
                .executeQuery(statement.sql)
        }
}