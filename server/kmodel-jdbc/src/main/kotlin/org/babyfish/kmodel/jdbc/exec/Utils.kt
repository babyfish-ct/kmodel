package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.metadata.Column
import org.babyfish.kmodel.jdbc.metadata.Table
import java.math.BigDecimal
import java.sql.Types

internal fun <T: AutoCloseable, R> T.using(handler: (T) -> R): R =
    try {
        handler(this)
    } finally {
        close()
    }

internal fun standardizeValue(
    value: Any,
    column: Column
): Any =
    when (column.type) {
        Types.CHAR ->
            when (value) {
                is String -> value
                else -> value.toString()
            }
        Types.VARCHAR ->
            when (value) {
                is String -> value
                else -> value.toString()
            }
        Types.NCHAR ->
            when (value) {
                is String -> value
                else -> value.toString()
            }
        Types.NVARCHAR ->
            when (value) {
                is String -> value
                else -> value.toString()
            }
        Types.CLOB ->
            when (value) {
                is String -> value
                else -> value.toString()
            }
        Types.NCLOB ->
            when (value) {
                is String -> value
                else -> value.toString()
            }
        Types.BOOLEAN ->
            when (value) {
                is Boolean -> value
                is Number -> value.toInt() != 0
                is String -> value == "true"
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.BOOLEAN"
                )
            }
        Types.TINYINT ->
            when (value) {
                is Byte -> value
                is Number -> value.toByte()
                is String -> value.toByte()
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.TINYINT"
                )
            }
        Types.SMALLINT ->
            when (value) {
                is Short -> value
                is Number -> value.toShort()
                is String -> value.toShort()
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.SMALLINT"
                )
            }
        Types.INTEGER ->
            when (value) {
                is Int -> value
                is Number -> value.toInt()
                is String -> value.toInt()
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.INTEGER"
                )
            }
        Types.BIGINT ->
            when (value) {
                is Long -> value
                is Number -> value.toLong()
                is String -> value.toLong()
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.BIGINT"
                )
            }
        Types.FLOAT ->
            when (value) {
                is Float -> value
                is Number -> value.toFloat()
                is String -> value.toFloat()
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.FLOAT"
                )
            }
        Types.DOUBLE ->
            when (value) {
                is Double -> value
                is Number -> value.toDouble()
                is String -> value.toDouble()
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.DOUBLE"
                )
            }
        Types.NUMERIC ->
            when (value) {
                is BigDecimal -> value
                is Number -> BigDecimal(value.toString())
                is String -> BigDecimal(value)
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.NUMERIC"
                )
            }
        Types.DECIMAL ->
            when (value) {
                is BigDecimal -> value
                is Number -> BigDecimal(value.toString())
                is String -> BigDecimal(value)
                else -> throw IllegalArgumentException(
                    "The value $value of column \"${column.name}\" " +
                            "does not match the types Types.DECIMAL"
                )
            }
        else ->
            throw IllegalArgumentException(
                "The column \"${column.name}\" " +
                        "is not valid type for primary key or conflict key"
            )
    }

internal fun <R> ExtraStatementBuilder.addConditionByPkValues(
    table: Table,
    rows: Collection<R>,
    pkValueGetter: (row: R, pkColumnIndex: Int, pkColumn: Column) -> Any
) {
    if (rows.isEmpty()) {
        throw IllegalArgumentException("rows cannot be empty")
    }
    if (rows.size == 1) {
        val row = rows.first()
        var addAnd = false
        for (pkColumnIndex in table.primaryKeyColumns.indices) {
            val pkColumn = table.primaryKeyColumns[pkColumnIndex]
            append(" and ", addAnd)
            append(pkColumn)
            append(" = ")
            appendExtraParam(
                pkValueGetter(row, pkColumnIndex, pkColumn),
                pkColumn.type
            )
            addAnd = true
        }
    } else {
        var addComma = false
        if (table.primaryKeyColumns.size == 1) {
            append(table.primaryKeyColumns[0])
        } else {
            append("(")
            for (pkColumn in table.primaryKeyColumns) {
                append(", ", addComma)
                append(pkColumn)
                addComma = true
            }
            append(")")
        }
        append(" in (")
        addComma = false
        for (row in rows) {
            append(", ", addComma)
            if (table.primaryKeyColumns.size == 1) {
                val pkColumn = table.primaryKeyColumns[0]
                appendExtraParam(
                    pkValueGetter(row, 0, pkColumn),
                    pkColumn.type
                )
            } else {
                append("(")
                var addNestedComma = false
                for (pkColumnIndex in table.primaryKeyColumns.indices) {
                    val pkColumn = table.primaryKeyColumns[pkColumnIndex]
                    append(", ", addNestedComma)
                    appendExtraParam(
                        pkValueGetter(row, pkColumnIndex, pkColumn),
                        pkColumn.type
                    )
                    addNestedComma = true
                }
                append(")")
            }
            addComma = true
        }
        append(")")
    }
}

internal fun mapRow(
    table: Table,
    columns: List<Column>,
    rsValueGetter: (columnIndex: Int) -> Any?
): Row {
    val pkValueMap = (table.primaryKeyColumns.indices).associateBy({
        table.primaryKeyColumns[it].name
    }) {
        rsValueGetter(it + 1) ?:
        error("Primary key does not support null value")
    }
    val otherValueMap = (table.primaryKeyColumnMap.size until columns.size)
        .associateBy({ columns[it].name }) {
            rsValueGetter(it + 1)
        }
    return Row(pkValueMap, otherValueMap)
}