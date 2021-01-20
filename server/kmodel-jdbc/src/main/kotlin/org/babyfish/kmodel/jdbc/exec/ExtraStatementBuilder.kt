package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.metadata.Column
import org.babyfish.kmodel.jdbc.metadata.Table
import org.babyfish.kmodel.jdbc.sql.Statement
import org.babyfish.kmodel.jdbc.sql.TokenRange
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.sql.PreparedStatement

internal class ExtraStatementBuilder(
    private val builder: StringBuilder = StringBuilder(),
    private val paramSequences: MutableList<ParamSequence> = mutableListOf()
) : Cloneable {

    private var frozen = false

    fun append(sqlPart: String) {
        validate()
        builder.append(sqlPart)
    }

    fun append(column: Column, tableAlias: String? = null) {
        validate()
        if (tableAlias !== null) {
            builder.append(tableAlias)
            builder.append('.')
        }
        builder.append(column.name)
    }

    fun append(columns: Collection<Column>, tableAlias: String? = null) {
        validate()
        var addComma = false
        for (column in columns) {
            append(", ", addComma)
            append(column, tableAlias)
            addComma = true
        }
    }

    fun append(table: Table) {
        validate()
        builder.append(table.qualifiedName.schema)
        builder.append('.')
        builder.append(table.qualifiedName.name)
    }

    fun append(sqlPart: String, condition: Boolean) {
        validate()
        if (condition) {
            builder.append(sqlPart)
        }
    }

    fun append(
            tokenRange: TokenRange?,
            statement: Statement,
            prefix: String? = null
    ) {
        validate()
        tokenRange
                ?.let { range ->
                    prefix?.let {
                        builder.append(it)
                    }
                    for (index in range.fromIndex until range.toIndex) {
                        builder.append(statement.tokens[index].text)
                    }
                    paramSequences += TokenRangeParamSequence(range)
                }
    }

    fun append(
        tokenRanges: List<TokenRange>,
        statement: Statement,
        prefix: String? = null
    ) {
        validate()
        var addComma = false
        tokenRanges.forEach {
            prefix?.let { p ->
                builder.append(p)
            }
            for (index in it.fromIndex until it.toIndex) {
                append(", ", addComma)
                builder.append(statement.tokens[index].text)
                addComma = true
            }
            paramSequences += TokenRangeParamSequence(it)
        }
    }

    fun appendExtraParam(value: Any?, sqlType: Int) {
        validate()
        builder.append("?")
        paramSequences += ExtraValueParamSequence(value, sqlType)
    }

    fun appendEqualities(
        columns: List<Column>,
        tableAlias: String? = null,
        rows: Collection<List<TokenRange>>,
        statement: Statement
    ) {
        appendEqualities(
            columns = columns,
            tableAlias = tableAlias,
            rows = rows
        ) { v, _ ->
            append(v, statement)
        }
    }

    fun appendEqualities(
        columns: List<Column>,
        tableAlias: String? = null,
        rows: Collection<List<Any?>>
    ) {
        appendEqualities(
            columns = columns,
            tableAlias = tableAlias,
            rows = rows
        ) { v, c ->
            appendExtraParam(v, c.type)
        }
    }

    private fun <V> appendEqualities(
        columns: List<Column>,
        tableAlias: String?,
        rows: Collection<List<V>>,
        valueAcceptor: (V, Column) -> Unit
    ) {
        validate()
        if (rows.isEmpty()) {
            append("1 = 0")
        } else if (columns.size == 1 && rows.size == 1) {
            append(columns[0], tableAlias)
            append(" = ")
            acceptEqualityRow(rows.first(), columns, valueAcceptor)
        } else if (columns.size == 1) {
            append(columns[0], tableAlias)
            append(" in (")
            var addComma = false
            for (row in rows) {
                append(", ", addComma)
                acceptEqualityRow(row, columns, valueAcceptor)
                addComma = true
            }
            append(")")
        } else {
            append("(")
            append(columns, tableAlias)
            append(") in (")
            var addComma = false
            for (row in rows) {
                append(", ", addComma)
                append("(")
                acceptEqualityRow(row, columns, valueAcceptor)
                append(")")
                addComma = true
            }
            append(")")
        }
    }

    private fun <V> acceptEqualityRow(
        row: List<V>,
        columns: List<Column>,
        valueAcceptor: (V, Column) -> Unit
    ) {
        if (row.size != columns.size) {
            throw IllegalArgumentException(
                "row.size must be equal to columns.size"
            )
        }
        var addComma = false
        for (i in row.indices) {
            append(", ", addComma)
            valueAcceptor(row[i], columns[i])
            addComma = true
        }
    }

    fun freeze() {
        frozen = true
    }

    fun build(): ExtraStatement =
        ExtraStatement(builder.toString(), paramSequences)

    public override fun clone(): ExtraStatementBuilder =
        ExtraStatementBuilder(
            builder = StringBuilder(builder.toString()),
            paramSequences = mutableListOf<ParamSequence>().also {
                it += paramSequences
            }
        )

    private inline fun validate() {
        if (frozen) {
            throw IllegalStateException("The current builder is frozen")
        }
    }

    private class TokenRangeParamSequence(
        private val tokenRange: TokenRange
    ) : ParamSequence {

        override fun applyTo(
            baseParamIndex: Int,
            targetStatement: PreparedStatement,
            originalParameterSetters: List<(PreparedStatement.(Int) -> Unit)?>?,
            extraValues: List<Any?>
        ): Int {
            var base = baseParamIndex
            var from = tokenRange.paramOffset
            var to = from + tokenRange.paramCount
            for (paramIndex in from until to) {
                originalParameterSetters?.get(paramIndex)?.let {
                    targetStatement.it(base++)
                }
            }
            return base
        }
    }

    private class ExtraValueParamSequence(
        private val value: Any?,
        private val sqlType: Int
    ) : ParamSequence {

        override fun applyTo(
            baseParamIndex: Int,
            targetStatement: PreparedStatement,
            originalParameterSetters: List<(PreparedStatement.(Int) -> Unit)?>?,
            extraValues: List<Any?>
        ): Int {
            if (value == null) {
                targetStatement.setNull(baseParamIndex, sqlType)
            } else {
                targetStatement.setObject(baseParamIndex, value, sqlType)
            }
            return baseParamIndex + 1
        }
    }
}