package org.babyfish.kmodel.jdbc.exec

data class Row(
    val pkValueMap: Map<String, Any>,
    val otherValueMap: Map<String, Any?>
) {
    val pkValues: List<Any> by lazy {
        pkValueMap.values.toList()
    }

    operator fun get(columnName: String): Any? =
        pkValueMap[columnName] ?: otherValueMap[columnName]
}