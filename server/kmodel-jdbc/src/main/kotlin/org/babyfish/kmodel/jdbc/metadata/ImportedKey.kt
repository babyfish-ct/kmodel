package org.babyfish.kmodel.jdbc.metadata

data class ImportedKey(
    val fkName: String,
    val parentTable: Table,
    val fkColumns: List<Column>
) {
    override fun toString(): String =
        "{parentTable: ${parentTable.name}, fkColumns = [${fkColumns}]}"
}