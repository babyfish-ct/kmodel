package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.metadata.Table

internal class TableImage(
        val table: Table
) {
    val rowPairs = mutableMapOf<List<Any?>, RowPair>()

    fun beforeRow(row: Row) {

    }

    class RowPair {

        val oldValues: MutableMap<String, Any>? = null

        val newValues: MutableMap<String, Any>? = null
    }
}