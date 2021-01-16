package org.babyfish.kmodel.jdbc.exec

import java.sql.PreparedStatement

data class Parameters(
    val values: List<Any?>,
    val setters: List<(PreparedStatement.(Int) -> Unit)?>
) {
    init {
        if (values.size != setters.size) {
            throw IllegalArgumentException(
                "values.size must equal setters.size"
            )
        }
    }
}

internal class MutableParameters {

    val values: MutableList<Any?> = mutableListOf()

    val setters: MutableList<(PreparedStatement.(Int) -> Unit)?> = mutableListOf()

    fun toReadonly(): Parameters =
        Parameters(
            values.toList(), //clone
            setters.toList() //clone
        )
}