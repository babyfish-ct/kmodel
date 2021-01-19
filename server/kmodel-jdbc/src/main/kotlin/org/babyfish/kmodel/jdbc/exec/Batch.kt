package org.babyfish.kmodel.jdbc.exec

data class Batch(
    val sql: String,
    val parameters: Parameters? = null
)