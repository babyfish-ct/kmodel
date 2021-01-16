package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.PreparedStatementProxy
import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.sql.SelectStatement
import org.babyfish.kmodel.jdbc.sql.illegalSql
import java.sql.ResultSet

class SelectPlan(
    private val statement: SelectStatement
) : ExecutionPlan<ResultSet> {

    override fun execute(
        statementProxy: StatementProxy,
        parameters: Parameters?
    ): ResultSet =
        if (parameters !== null) {
            statementProxy
                .targetCon
                .prepareStatement(statement.sql)
                .using {
                    for (parameterIndex in 1 until parameters.setters.size) {
                        parameters.setters[parameterIndex]
                            ?.let { setter ->
                                it.setter(parameterIndex)
                            }
                            ?: illegalSql(
                                statement.sql,
                                "Parameter $parameterIndex is not set"
                            )
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