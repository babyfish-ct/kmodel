package org.babyfish.kmodel.jdbc.exec

internal data class Batch(
    val sql: String,
    val parameters: Parameters? = null
)