package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.sql.TokenRange
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement

class ExtraStatement internal constructor(
    private val sql: String,
    private val paramSequences: List<ParamSequence>
) {
    fun executeQuery(
        targetConnection: Connection,
        parameterSetters: List<(PreparedStatement.(Int) -> Unit)?>?,
        mapper: ((columnIndex: Int) -> Any?) -> Row
    ): MutableMap<List<Any>, Row> {
        LOGGER.info("Extra SQL: $sql")
        return if (paramSequences.isNotEmpty()) {
            targetConnection
                .prepareStatement(sql)
                .using {
                    var queryParamIndex = 1
                    for (paramSequence in paramSequences) {
                        queryParamIndex = paramSequence.applyTo(
                            queryParamIndex,
                            it,
                            parameterSetters,
                            emptyList()
                        )
                    }
                    mutableMapOf<List<Any>, Row>().apply {
                        val rs = it.executeQuery()
                        while (rs.next()) {
                            val row = mapper(rs::getObject)
                            this[row.pkValues] = row
                        }
                    }
                }
        } else {
            mutableMapOf<List<Any>, Row>().apply {
                val rs = targetConnection
                    .createStatement()
                    .executeQuery(sql)
                while (rs.next()) {
                    val row = mapper(rs::getObject)
                    this[row.pkValues] = row
                }
            }
        }
    }

    fun executeUpdate(
        targetConnection: Connection,
        parameterSetters: List<(PreparedStatement.(Int) -> Unit)?>?
    ): Int {
        LOGGER.info("Extra SQL: $sql")
        return if (paramSequences.isNotEmpty()) {
            targetConnection
                .prepareStatement(sql)
                .using {
                    var queryParamIndex = 1
                    for (paramSequence in paramSequences) {
                        queryParamIndex = paramSequence.applyTo(
                            queryParamIndex,
                            it,
                            parameterSetters,
                            emptyList()
                        )
                    }
                    it.executeUpdate()
                }
        } else {
            targetConnection
                .createStatement()
                .executeUpdate(sql)
        }
    }
}

internal interface ParamSequence {

    fun applyTo(
        baseParamIndex: Int,
        targetStatement: PreparedStatement,
        originalParameterSetters: List<(PreparedStatement.(Int) -> Unit)?>?,
        extraValues: List<Any?>
    ): Int
}

private val LOGGER = LoggerFactory.getLogger(ExtraStatement::class.java)