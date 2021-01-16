package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.sql.Statement

internal fun mutate(
    statementProxy: StatementProxy,
    statement: Statement,
    parameters: Parameters?
): Int =
    if (parameters !== null) {
        statementProxy
                .targetCon
                .prepareStatement(statement.sql)
                .using {
                    for (parameterIndex in 1 until parameters.setters.size) {
                        parameters
                                .setters[parameterIndex]
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
