package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.PreparedStatementProxy
import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.sql.Statement

internal fun mutate(
        statementProxy: StatementProxy,
        statement: Statement
): Int =
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
                        it.executeUpdate()
                    }
        } else {
            statementProxy
                    .targetCon
                    .createStatement()
                    .executeUpdate(statement.sql)
        }
