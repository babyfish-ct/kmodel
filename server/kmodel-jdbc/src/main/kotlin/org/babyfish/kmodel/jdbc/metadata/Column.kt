package org.babyfish.kmodel.jdbc.metadata

import java.sql.JDBCType

data class Column(
    val name: String,
    val type: Int
) {
    internal lateinit var _table: Table

    val table: Table
        get() = _table

    override fun toString(): String {
        return "{name = $name, type = ${JDBCType.valueOf(type).name}}"
    }
}