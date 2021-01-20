package org.babyfish.kmodel.jdbc.metadata

data class ForeignKey(
    val name: String,
    val childColumns: List<Column>,
    val parentColumns: List<Column>
) {
    val childTable: Table
        get() = childColumns.first().table

    val parentTable: Table
        get() = parentColumns.first().table
}

internal class ForeignKeyListBuilder {
    private val fkBuilderMap =
        mutableMapOf<String, ForeignKeyBuilder>()

    fun append(
        name: String,
        childTable: Table,
        parentTable: Table,
        childTableColumnName: String,
        parentTableColumnName: String
    ) {
        fkBuilderMap.computeIfAbsent(name) {
            ForeignKeyBuilder(
                it,
                childTable = childTable,
                parentTable = parentTable
            )
        }.append(
            childTableColumnName = childTableColumnName,
            parentTableColumnName = parentTableColumnName
        )
    }

    fun build(): List<ForeignKey> =
        fkBuilderMap.values.map { it.build() }

    private inner class ForeignKeyBuilder(
        private val name: String,
        private val childTable: Table,
        private val parentTable: Table
    ) {
        private val childTableColumns = mutableListOf<Column>()

        private val parentTableColumns = mutableListOf<Column>()

        fun append(
            childTableColumnName: String,
            parentTableColumnName: String
        ) {
            childTableColumns += childTable.columnMap[childTableColumnName]
                ?: error("Internal bug")
            parentTableColumns += parentTable.columnMap[parentTableColumnName]
                ?: error("Internal bug")
        }

        fun build(): ForeignKey =
            ForeignKey(
                name = name,
                childColumns = childTableColumns,
                parentColumns = parentTableColumns
            )
    }
}