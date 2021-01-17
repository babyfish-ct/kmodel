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