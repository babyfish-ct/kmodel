package org.babyfish.kmodel.config

fun <M: Any, T> PropBuilder<M, T>.sql(
    action: PropSqlConfigBuilder<M, T>.() -> Unit
) {
    this.config(action)
}

fun <M: Any, T> PropBuilder<M, T>.mongo(
    action: PropSqlConfigBuilder<M, T>.() -> Unit
) {
    this.config(action)
}

class PropSqlConfigBuilder<M: Any, T> : PropConfigBuilder<M, T>() {

    fun column(name: String, length: Int = -1) {}
}