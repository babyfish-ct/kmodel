package org.babyfish.kmodel.jdbc.metadata

data class QualifiedName(
        val catalog: String,
        val schema: String,
        val name: String
) {
    override fun toString(): String =
            "${catalog}.${schema}.${name}"
}