package org.babyfish.kmodel.config

class ModelSqlBuilder<M: Any> {

    fun tableName(name: String) {

    }
}

fun <M: Any> ModelBuilder<M>.sql(
    sqlActionBuilderAction: ModelSqlBuilder<M>.() -> Unit
) {

}