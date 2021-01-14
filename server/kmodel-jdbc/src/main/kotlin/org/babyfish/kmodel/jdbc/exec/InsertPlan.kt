package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.PreparedStatementProxy
import org.babyfish.kmodel.jdbc.SqlLexer
import org.babyfish.kmodel.jdbc.StatementProxy
import org.babyfish.kmodel.jdbc.metadata.Column
import org.babyfish.kmodel.jdbc.sql.InsertStatement
import org.babyfish.kmodel.jdbc.sql.TokenRange
import org.babyfish.kmodel.jdbc.sql.illegalSql
import java.sql.Connection
import java.sql.SQLException

class InsertPlan(
    con: Connection,
    statement: InsertStatement
) : AbstractDMLMutationPlan<InsertStatement>(
    con,
    statement
) {
    private val conflictKeyColumns: List<Column> =
        if (statement.conflictPolicy?.constraintName === null) {
            table.primaryKeyColumnMap.values.toList()
        } else {
            TODO()
        }

    private val primaryKeyValueIndices: List<Int> =
        uniqueColumnIndicates(table.primaryKeyColumnMap.values)

    private val conflictKeyValueIndices: List<Int> =
        uniqueColumnIndicates(conflictKeyColumns)

    private val insertStatementBuilderTemplate =
        ExtraStatementBuilder().apply {
            append("insert into ")
            append(statement.tableSourceRange, statement)
            append("(", statement.insertedColumnRanges.isNotEmpty())
            append(statement.insertedColumnRanges, statement)
            append(")", statement.insertedColumnRanges.isNotEmpty())
            append(" values ")
            freeze()
        }

    private fun uniqueColumnIndicates(
        uniqueColumns: Collection<Column>
    ): List<Int> =
        uniqueColumns.map {
            val index = columns.indexOf(it)
            if (index === -1) {
                val token = statement.insertedColumnRanges.lastOrNull()
                    ?.let { tr ->
                        statement.tokens[tr.fromIndex]
                    }
                    ?: statement.tokens[0]
                illegalSql(
                    statement.fullSql,
                    "Unique column \"${it.name}\" is not inserted",
                    token
                )
            }
            index
        }

    override fun determineColumns(): List<Column> =
        statement.insertedColumnRanges.let {
            if (it.isEmpty()) {
                table.columnMap.values.toList()
            } else {
                it.map { tr ->
                    val token = statement.tokens[tr.fromIndex]
                    table.getColumn(token.text)
                        ?: illegalSql(
                            statement.fullSql,
                            "Unknown inserted column \"${token.text}\"",
                            token
                        )
                }
            }
        }

    override fun ExtraStatementBuilder.determineImageQueryCondition() {
        val conflictKeyValueLists = statement.rows.map { row ->
            conflictKeyValueIndices.map {
                row.values[it].also { tr ->
                    if (!isLiteral(tr)) {
                        illegalSql(
                            statement.fullSql,
                            "The inserted primary key value cannot be complex expression",
                            statement.tokens[tr.fromIndex]
                        )
                    }
                }
            }
        }
        val compositeKey = conflictKeyColumns.size > 1
        append(" where ")
        append("(", compositeKey)
        append(conflictKeyColumns.joinToString() { it.name })
        append(")", compositeKey)
        if (!compositeKey && conflictKeyValueLists.size == 1) {
            append(" = ")
            append(conflictKeyValueLists[0][0], statement)
        } else {
            append(" in (")
            var addComma = false
            for (primaryKeyValueList in conflictKeyValueLists) {
                append(", ", addComma)
                append("(", compositeKey)
                append(primaryKeyValueList, statement)
                append(")", compositeKey)
                addComma = true
            }
            append(")")
        }
    }

    override fun mapExtraRow(rsValueGetter: (columnIndex: Int) -> Any?): Row {
        val pkValueMap = table.primaryKeyColumns.indices.associateBy({
            table.primaryKeyColumns[it].name
        }) {
            rsValueGetter(primaryKeyValueIndices[it] + 1) ?:
                    error("Primary key does not support null value")
        }
        val otherValueMap = columns
            .indices
            .minus(primaryKeyValueIndices)
            .associateBy({ columns[it].name }) {
                rsValueGetter(it + 1)
            }
        return Row(pkValueMap, otherValueMap)
    }

    override fun execute(statementProxy: StatementProxy): DMLMutationResult {
        val conflictPolicy = statement.conflictPolicy
        val beforeRowMap = if (conflictPolicy === null) {
            mutableMapOf()
        } else {
            imageQuery.executeQuery(
                statementProxy.targetCon,
                (statementProxy as? PreparedStatementProxy)?.parameterSetters
            ) {
                mapExtraRow(it)
            }
        }
        val insertMap = statement
            .rows
            .associateByTo(mutableMapOf()) {
                conflictKeyValueIndices.mapIndexed { keyIndex, valueIndex ->
                    literal(
                        it.values[valueIndex],
                        (statementProxy as? PreparedStatementProxy)?.parameters
                    ).let {
                        standardizeValue(
                            it,
                            conflictKeyColumns[keyIndex]
                        )
                    }
                }
            }
        val updateCount = if (conflictPolicy === null) {
            mutate(statementProxy, statement)
        } else {
            val updateMap = mutableMapOf<List<Any?>, InsertStatement.Row>()
            for (beforeRow in beforeRowMap.values) {
                val rowKey = conflictKeyColumns.map {
                    beforeRow[it.name]
                        ?.let { value ->
                            standardizeValue(
                                value,
                                it
                            )
                        }
                }
                insertMap.remove(rowKey)?.let {
                    updateMap[rowKey] = it
                }
            }
            val insertCount = if (insertMap.isEmpty()) {
                0
            } else {
                insertStatementBuilderTemplate
                    .clone()
                    .apply {
                        for (row in insertMap.values) {
                            append("(")
                            append(row.values, statement)
                            append(")")
                        }
                    }
                    .build()
                    .executeUpdate(
                        statementProxy.targetCon,
                        (statementProxy as? PreparedStatementProxy)
                            ?.parameterSetters
                            ?: emptyList()
                    )
            }
            val updateCount = if (updateMap.isEmpty()) {
                0
            } else {
                updateMap
                    .entries
                    .map { (rowKey, row) ->
                        updateStatement(rowKey, row)
                            ?.executeUpdate(
                                statementProxy.targetCon,
                                (statementProxy as? PreparedStatementProxy)
                                    ?.parameterSetters
                                    ?: emptyList()
                            )
                            ?: 0
                    }
                    .sum()
            }
            insertCount + updateCount
        }
        return DMLMutationResult(
            table = table,
            updateCount = updateCount,
            beforeRowMap = beforeRowMap.let {
                val map = it as MutableMap<List<Any>, Row?>
                for (pkValues in insertMap.keys) {
                    map[pkValues] = null
                }
                map
            }
        )
    }

    private inline fun isLiteral(
        tokenRange: TokenRange
    ): Boolean {
        if (tokenRange.fromIndex + 1 != tokenRange.toIndex) {
            return false
        }
        val token = statement.tokens[tokenRange.fromIndex]
        return token.type == SqlLexer.STRING ||
                token.type == SqlLexer.NUMBER ||
                token.text == "?"
    }

    private inline fun literal(
        tokenRange: TokenRange,
        parameters: List<Any?>?
    ): Any {
        val token = statement.tokens[tokenRange.fromIndex]
        return when (token.type) {
            SqlLexer.NUMBER -> token.text
            SqlLexer.STRING -> token.text.let {
                it.substring(1, it.length - 1)
            }
            SqlLexer.SYMBOL -> {
                if (token.text != "?" || parameters === null) {
                    error("Internal bug")
                }
                parameters[tokenRange.paramOffset] ?:
                        throw SQLException(
                            "The parameter ${tokenRange.paramOffset} is not set"
                        )
            }
            else -> error("Internal bug")
        }
    }

    private fun updateStatement(
        rowKey: List<Any?>,
        row: InsertStatement.Row
    ): ExtraStatement? =
        statement
            ?.conflictPolicy
            ?.updatedActions
            ?.takeIf {
                it.isNotEmpty()
            }
            ?.let {
                ExtraStatementBuilder().apply {
                    append("update ")
                    append(table.name)
                    append(" set ")
                    var addComma = false
                    for (updatedAction in it) {
                        val columnToken =
                            statement.tokens[updatedAction.columnRange.toIndex - 1]
                        val column =
                            table.getColumn(columnToken.text)
                                ?: illegalSql(
                                    statement.fullSql,
                                    "Illegal updated column \"${columnToken.text}\"",
                                    columnToken
                                )
                        if (table.isPrimaryKey(column.name)) {
                            illegalSql(
                                statement.fullSql,
                                "Cannot update the primary key \"${columnToken.text}\"",
                                columnToken
                            )
                        }
                        if (conflictKeyColumns.contains(column)) {
                            illegalSql(
                                statement.fullSql,
                                "Cannot update the conflict key \"${columnToken.text}\"",
                                columnToken
                            )
                        }
                        append(", ", addComma)
                        append(column.name)
                        append(" = ")
                        appendUpdatedValue(column, updatedAction.valueRange, row)
                        addComma = true
                    }
                    append(" where ")
                    var addAnd = false
                    for (index in conflictKeyColumns.indices) {
                        append(" and ", addAnd)
                        append(conflictKeyColumns[index].name)
                        append(" = ")
                        appendExtraParam(rowKey[index], conflictKeyColumns[index].type)
                        addAnd = true
                    }
                }.build()
            }

    private fun ExtraStatementBuilder.appendUpdatedValue(
        column: Column,
        tokenRange: TokenRange,
        row: InsertStatement.Row
    ) {
        val specialUpdatedValues = mutableListOf<SpecialUpdatedValue>()
        var channel = UpdatedValueChannel.NORMAL
        var channelIndex = -1
        var channelSize = 0
        var channelIdentifier = ""
        for (tokenIndex in tokenRange.fromIndex until tokenRange.toIndex) {
            val token = statement.tokens[tokenIndex]
            if (token.type == SqlLexer.WS || token.type == SqlLexer.COMMENT) {
                continue
            }
            when (channel) {
                UpdatedValueChannel.NORMAL ->
                    when {
                        token.text.equals("values", true) -> {
                            channel = UpdatedValueChannel.VALUES
                            channelIndex = tokenIndex
                            channelSize = 0
                            channelIdentifier = ""
                        }
                        token.text.equals("excluded", true) -> {
                            channel = UpdatedValueChannel.EXCLUDED
                            channelIndex = tokenIndex
                            channelSize = 0
                            channelIdentifier = ""
                        }
                    }
                UpdatedValueChannel.VALUES ->
                    when (channelSize) {
                        0 -> if (token.text == "(") {
                            channelSize = 1
                        } else {
                            channel = UpdatedValueChannel.NORMAL
                        }
                        1 -> if (token.type == SqlLexer.IDENTIFIER) {
                            channelSize = 2
                            channelIdentifier = token.text
                        } else {
                            channel = UpdatedValueChannel.NORMAL
                        }
                        2 -> {
                            specialUpdatedValues += SpecialUpdatedValue(
                                fromTokenIndex = channelIndex,
                                toTokenIndex = tokenIndex + 1,
                                identifier = channelIdentifier
                            )
                            channel = UpdatedValueChannel.NORMAL
                        }
                    }
                UpdatedValueChannel.EXCLUDED ->
                    when (channelSize) {
                        0 -> if (token.text == ".") {
                            channelSize = 1
                        } else {
                            channel = UpdatedValueChannel.NORMAL
                        }
                        1 -> {
                            if (token.type == SqlLexer.IDENTIFIER) {
                                specialUpdatedValues += SpecialUpdatedValue(
                                    fromTokenIndex = channelIndex,
                                    toTokenIndex = tokenIndex + 1,
                                    identifier = channelIdentifier
                                )
                            }
                            channel = UpdatedValueChannel.NORMAL
                        }
                    }
            }
        }
        if (specialUpdatedValues.isEmpty()) {
            append(tokenRange, statement)
        } else {
            var tokenIndex = tokenRange.fromIndex
            for (specialUpdatedValue in specialUpdatedValues) {
                append(
                    statement.createTokenRange(
                        tokenIndex, specialUpdatedValue.fromTokenIndex
                    ),
                    statement
                )
                tokenIndex = specialUpdatedValue.toTokenIndex
                val column = specialUpdatedValue
                    .identifier
                    .let {
                        table.getColumn(it)
                            ?: illegalSql(
                                statement.fullSql,
                                "Unknown inserted column \"$it\"",
                                statement.tokens[specialUpdatedValue.fromTokenIndex]
                            )
                    }
                val insertedColumnIndex = columns.indexOf(column)
                if (insertedColumnIndex == -1) {
                    illegalSql(
                        statement.fullSql,
                        "Unknown inserted column \"${column.name}\"",
                        statement.tokens[specialUpdatedValue.fromTokenIndex]
                    )
                }
                append(row.values[insertedColumnIndex], statement)
                append(" ")
            }
            if (tokenIndex < tokenRange.toIndex) {
                append(
                    statement.createTokenRange(
                        tokenIndex, tokenRange.toIndex
                    ),
                    statement
                )
            }
        }
    }

    private enum class UpdatedValueChannel {
        NORMAL,
        VALUES,
        EXCLUDED
    }

    private class SpecialUpdatedValue(
        val fromTokenIndex: Int,
        val toTokenIndex: Int,
        val identifier: String
    )
}
