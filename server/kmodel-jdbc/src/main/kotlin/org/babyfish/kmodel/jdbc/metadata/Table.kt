package org.babyfish.kmodel.jdbc.metadata

data class Table(
        val qualifiedName: QualifiedName,
        val columnMap: Map<String, Column>,
        val insensitiveColumnMap: Map<String, Column>,
        val primaryKeyColumnMap: Map<String, Column>,
        val insensitivePrimaryKeyColumnMap: Map<String, Column>
) {

    internal lateinit var _importedForeignKeys: List<ForeignKey>

    internal lateinit var _exportedForeignKeys: List<ForeignKey>

    val catalog: String
        get() = qualifiedName.catalog

    val schema: String
        get() = qualifiedName.schema

    val name: String
        get() = qualifiedName.name

    val importedForeignKeys: List<ForeignKey>
        get() = _importedForeignKeys

    val exportedForeignKeys: List<ForeignKey>
        get() = _exportedForeignKeys

    fun getColumn(name: String): Column? =
        name.standardIdentifier().let {
            columnMap[it] ?: insensitiveColumnMap[it.toUpperCase()]
        }

    fun isPrimaryKey(name: String): Boolean =
            name.standardIdentifier().let {
                primaryKeyColumnMap.containsKey(it) || insensitivePrimaryKeyColumnMap.containsKey(name)
            }

    val primaryKeyColumns : List<Column> by lazy {
        primaryKeyColumnMap.values.toList()
    }

    override fun toString(): String =
        "{qualifiedName: $qualifiedName, " +
                "columns: [${columnMap.values}], " +
                "importedForeignKeys: [${_importedForeignKeys}], " +
                "exportedForeignKeys: [${_exportedForeignKeys}]}"
}