package org.babyfish.kmodel.jdbc.metadata

data class ExportedKey(
    val fkName: String,
    val childTable: Table,
    val fkColumns: List<Column>
) {
    override fun toString(): String =
        "{childTable: ${childTable.name}, fkColumns = [${fkColumns}]}"
}
