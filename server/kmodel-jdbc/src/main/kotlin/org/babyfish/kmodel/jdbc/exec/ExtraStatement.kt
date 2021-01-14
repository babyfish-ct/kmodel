package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.sql.TokenRange
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
    ): MutableMap<List<Any>, Row> =
        if (parameterSetters !== null) {
            println(sql)
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

    fun executeUpdate(
        targetConnection: Connection,
        parameterSetters: List<(PreparedStatement.(Int) -> Unit)?>?
    ): Int =
        if (parameterSetters !== null) {
            println(sql)
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

    private fun applyParameters(
        targetStatement: PreparedStatement,
        tokenRange: TokenRange,
        baseParamIndex: Int,
        parameterSetters: List<(PreparedStatement.(Int) -> Unit)?>
    ): Int {
        var base = baseParamIndex
        var from = tokenRange.paramOffset
        var to = from + tokenRange.paramCount
        for (paramIndex in from until to) {
            parameterSetters[paramIndex]?.let {
                targetStatement.it(base++)
            }
        }
        return base
    }
}

internal interface ParamSequence {

    fun applyTo(
        baseParamIndex: Int,
        targetStatement: PreparedStatement,
        originalParameterSetters: List<(PreparedStatement.(Int) -> Unit)?>,
        extraValues: List<Any?>
    ): Int
}
