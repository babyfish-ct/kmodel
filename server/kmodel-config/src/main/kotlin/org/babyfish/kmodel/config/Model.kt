package org.babyfish.kmodel.config

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

abstract class Model<M: Any> private constructor(
    private val modelType: KClass<M>
) {
    fun <T> id(
        prop: KProperty1<M, T>,
        propBuilderAction: PropBuilder<M, T>.() -> Unit
    ) {
        TODO()
    }

    companion object {

        fun <M: Any> create(modelType: KClass<M>, modelBuilderAction: ModelBuilder<M>.() -> Unit): Model<M> {
            TODO()
        }
    }
}