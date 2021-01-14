package org.babyfish.kmodel.jdbc.metadata

import org.babyfish.kmodel.jdbc.exec.using
import java.lang.IllegalArgumentException
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.SQLException
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Pattern
import kotlin.concurrent.read
import kotlin.concurrent.write

class TableManager internal constructor(
    val connectionString: String,
    val userName: String
) {

    private val readWriteLock = ReentrantReadWriteLock()

    private val tableMap = mutableMapOf<QualifiedName, Table>()

    private val insensitiveTableMap = mutableMapOf<QualifiedName, Table>()

    operator fun get(con: Connection, name: String): Table {
        val conUrl = con.metaData.url
        val conUser = con.metaData.userName
        if (connectionString != conUrl) {
            throw IllegalArgumentException(
                "The table manager can only be used by \"$connectionString\", " +
                        "not \"$conUrl\"")
        }
        if (userName != conUser) {
            throw IllegalArgumentException(
                "The table manager can only be used by the user \"$userName\", " +
                        "not \"$conUser\"")
        }
        val qualifiedName = qualifiedNameOf(con, name)
        val insensitiveQualifiedName = qualifiedName.insensitiveQualifiedName()
        return readWriteLock.read {
            tableMap[qualifiedName] ?: insensitiveTableMap[insensitiveQualifiedName]
        } ?: readWriteLock.write {
            tableMap[qualifiedName]
                ?: insensitiveTableMap[insensitiveQualifiedName]
                ?: create(con, qualifiedName)
                    .also {
                        tableMap += it
                        for ((k, v) in it) {
                            insensitiveTableMap[k.rename(k.name.toUpperCase())] = v
                        }
                    }
                    .values
                    .first()
                    // use first() to retrieve the firstly created table
                    // because kotlin's API always create order map(java's LinkedHashMap)
        }
    }

    private fun create(
        con: Connection,
        qualifiedName: QualifiedName
    ): Map<QualifiedName, Table> =
        mutableMapOf<QualifiedName, Table>().also {
            getOrCreate(con, qualifiedName, it)
        }

    private fun getOrCreate(
        con: Connection,
        qualifiedName: QualifiedName,
        outputMap: MutableMap<QualifiedName, Table>
    ): Table {
        val existingTable = outputMap[qualifiedName] ?: tableMap[qualifiedName]
        if (existingTable !== null) {
            return existingTable
        }
        val metaData = con.metaData
        val tableName =
                metaData
                        .tryGetStandardTableName(
                                qualifiedName.catalog,
                                qualifiedName.schema,
                                qualifiedName.name
                        )
                        ?: metaData
                                .tryGetStandardTableName(
                                        qualifiedName.catalog,
                                        qualifiedName.schema,
                                        qualifiedName.name.toUpperCase()
                                ) ?: throw SQLException("There is not table \"${qualifiedName}\"")
        val (columnMap, insensitiveColumnMap) = metaData
                .getColumns(
                        qualifiedName.catalog,
                        qualifiedName.schema,
                        tableName,
                        null
                )
                .using {
                    val columnMap = mutableMapOf<String, Column>()
                    val insensitiveColumnMap = mutableMapOf<String, Column>()
                    while (it.next()) {
                        val name = it.getString("COLUMN_NAME")
                        val type = it.getInt("DATA_TYPE")
                        val column = Column(name, type)
                        columnMap[name] = column
                        insensitiveColumnMap[name.toUpperCase()] = column
                    }
                    columnMap to insensitiveColumnMap
                }
        val (primaryKeyColumnMap, insensitivePrimaryKeyColumnMap) = metaData
                .getPrimaryKeys(
                        qualifiedName.catalog,
                        qualifiedName.schema,
                        tableName
                )
                .using {
                    val primaryKeyColumnMap = mutableMapOf<String, Column>()
                    val insensitivePrimaryKeyColumnMap = mutableMapOf<String, Column>()
                    while (it.next()) {
                        val name = it.getString("COLUMN_NAME")
                        val column = columnMap[name]!!
                        primaryKeyColumnMap[name] = column
                        insensitivePrimaryKeyColumnMap[name.toUpperCase()] = column
                    }
                    primaryKeyColumnMap to insensitivePrimaryKeyColumnMap
                }
        val table = Table(
                qualifiedName.rename(tableName),
                columnMap = columnMap,
                insensitiveColumnMap = insensitiveColumnMap,
                primaryKeyColumnMap = primaryKeyColumnMap,
                insensitivePrimaryKeyColumnMap = insensitivePrimaryKeyColumnMap
        )
        for (column in columnMap.values) {
            column._table = table
        }
        outputMap[table.qualifiedName] = table
        table._importedForeignKeys = metaData
            .getImportedKeys(
                qualifiedName.catalog,
                qualifiedName.schema,
                table.name
            )
            .using {
                val fkListBuilder = ForeignKeyListBuilder()
                while (it.next()) {
                    val fkName = it.getString("FK_NAME")
                    val fkColumnName = it.getString("FKCOLUMN_NAME")
                    val pkColumnName = it.getString("PKCOLUMN_NAME")
                    val parentQualifiedName = QualifiedName(
                        catalog = it.getString("PKTABLE_CAT"),
                        schema = it.getString("PKTABLE_SCHEM"),
                        name = it.getString("PKTABLE_NAME")
                    )
                    val parentTable = getOrCreate(con, parentQualifiedName, outputMap)
                    fkListBuilder.append(
                        name = fkName,
                        childTable = table,
                        parentTable = parentTable,
                        childTableColumnName = fkColumnName,
                        parentTableColumnName = pkColumnName
                    )
                }
                fkListBuilder.build()
            }
        table._exportedForeignKeys = metaData
            .getExportedKeys(
                qualifiedName.catalog,
                qualifiedName.schema,
                table.name
            )
            .using {
                val fkListBuilder = ForeignKeyListBuilder()
                val illegalFkNames = mutableSetOf<String>()
                while (it.next()) {
                    val fkName = it.getString("FK_NAME")
                    val childQualifiedName = QualifiedName(
                        catalog = it.getString("FKTABLE_CAT"),
                        schema = it.getString("FKTABLE_SCHEM"),
                        name = it.getString("FKTABLE_NAME")
                    )
                    val childTable = getOrCreate(con, childQualifiedName, outputMap)
                    val fkColumnName = it.getString("FKCOLUMN_NAME")
                    val pkColumnName = it.getString("PKCOLUMN_NAME")
                    fkListBuilder.append(
                        name = fkName,
                        childTable = childTable,
                        parentTable = table,
                        childTableColumnName = fkColumnName,
                        parentTableColumnName = pkColumnName
                    )
                    val deleteRule = it.getShort("DELETE_RULE").toInt()
                    if (deleteRule != DatabaseMetaData.importedKeyNoAction &&
                        deleteRule != DatabaseMetaData.importedKeyRestrict) {
                        illegalFkNames += fkName
                    }
                }
                if (illegalFkNames.isNotEmpty()) {
                    throw SQLException(
                        "Illegal foreign key constraints [${
                                illegalFkNames.joinToString { fkn ->
                                    "${qualifiedName.catalog}.${qualifiedName.schema}.$fkn"
                                }
                                }], 'on delete set null' or 'on delete cascade' " +
                                "are not supported by kmodel-jdbc"
                    )
                }
                fkListBuilder.build()
            }
        return table
    }

    private fun QualifiedName.rename(name: String): QualifiedName =
            if (this.name == name) {
                this
            } else {
                QualifiedName(
                        catalog = catalog,
                        schema = schema,
                        name = name
                )
            }

    private fun QualifiedName.insensitiveQualifiedName(): QualifiedName =
            rename(name.toUpperCase())

    private fun DatabaseMetaData.tryGetStandardTableName(
            catalog: String,
            schema: String,
            tableName: String
    ): String? =
            getTables(
                    null,
                    null,
                    tableName,
                    arrayOf("TABLE")
            ).using {
                while (it.next()) {
                    val c = it.getString("TABLE_CAT")
                    val s = it.getString("TABLE_SCHEM")
                    if (c.equals(catalog, true) && s.equals(schema, true)) {
                        return@using it.getString("TABLE_NAME")
                    }
                }
                null
            }
}

private val tableManagerLock = ReentrantReadWriteLock()

private val tableManagerMap = mutableMapOf<Pair<String, String>, TableManager>()

fun tableManager(con: Connection): TableManager {
    val metaData = con.metaData
    val tableManagerKey = metaData.url to metaData.userName
    return tableManagerLock.read {
        tableManagerMap[tableManagerKey]
    } ?: tableManagerLock.write {
        tableManagerMap[tableManagerKey] ?:
                TableManager(
                    tableManagerKey.first,
                    tableManagerKey.second
                ).also {
                    tableManagerMap[tableManagerKey] = it
                }
    }
}

fun evictTableManagers() {
    tableManagerLock.write {
        tableManagerMap.clear()
    }
}

private val DOT_PATTERN = Pattern.compile("\\s*\\.\\s*")

private fun qualifiedNameOf(con: Connection, qualifiedName: String): QualifiedName {
    val names = DOT_PATTERN.split(qualifiedName)
    val name = names[names.size - 1].standardIdentifier()
    val schema = if (names.size >= 2) {
        names[names.size - 2].standardIdentifier()
    } else {
        con.schema
    }
    val catalog = if (names.size >= 3) {
        names[names.size - 3].standardIdentifier()
    } else {
        con.catalog
    }
    return QualifiedName(catalog, schema, name)
}