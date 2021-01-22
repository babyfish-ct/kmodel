package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.Configuration
import org.babyfish.kmodel.jdbc.ConnectionProxy
import org.babyfish.kmodel.jdbc.DataChangedEvent
import org.babyfish.kmodel.jdbc.metadata.QualifiedName
import org.babyfish.kmodel.jdbc.metadata.Table
import org.babyfish.kmodel.jdbc.metadata.tableManager

internal class RootExecutionContext(
    cfg: Configuration
) : ExecutionContext(cfg, mutableMapOf()) {

    fun commit(conProxy: ConnectionProxy) {
        val tableMap = mutableMapOf<QualifiedName, Table>()
        val afterImageMap = mutableMapOf<
                QualifiedName, //tableName
                Map<List<Any>, Row>
                >()

        val tableManager = tableManager(conProxy.target)
        for ((qualifiedName) in beforeImageMap) {
            val table = tableManager[conProxy.target, qualifiedName.toString()]
            tableMap[qualifiedName] = table
            afterImageMap[qualifiedName] = afterRowMap(table, conProxy)
        }
        cfg.dataChangedListener(
            DataChangedEvent(
                beforeImageMap = beforeImageMap,
                afterImageMap = afterImageMap
            )
        )
    }

    private fun afterRowMap(
        table: Table,
        conProxy: ConnectionProxy
    ) : Map<List<Any>, Row> {
        val beforeRowMap = beforeImageMap[table.qualifiedName]
            ?: return emptyMap()
        return ExtraStatementBuilder()
            .apply {
                append("select ")
                append(table.columnMap.values)
                append(" from ")
                append(table)
                append(" where ")
                appendEqualities(
                    columns = table.primaryKeyColumns,
                    rows = beforeRowMap.keys
                )
            }
            .build()
            .executeQuery(
                conProxy.target,
                emptyList()
            ) {
                mapRow(
                    table,
                    table.columnMap.values.toList(),
                    it
                )
            }
    }
}