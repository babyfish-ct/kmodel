package org.babyfish.kmodel.config

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class ModelBuilder<M: Any> private constructor(
    val modelType: KClass<M>
) {
    fun filter(
        filterAction: FilterBuilder.() -> Unit
    ) {

    }

    fun <T> id(
        prop: KProperty1<M, T>,
        propBuilderAction: (PropBuilder<M, T>.() -> Unit)?= null
    ) {

    }

    fun <T> version(
        prop: KProperty1<M, T>,
        propBuilderAction: (PropBuilder<M, T>.() -> Unit)?= null
    ) {

    }

    fun <T> value(
        prop: KProperty1<M, T>,
        propBuilderAction: (PropBuilder<M, T>.() -> Unit)?= null
    ) {

    }

    fun <R: Any> toOne(
        prop: KProperty1<M, R>,
        propBuilderAction: (ToOnePropBuilder<M, R>.() -> Unit)?= null
    ) {

    }

    fun <E: Any> oneToMany(
        prop: KProperty1<M, List<E>>,
        propBuilderAction: (OneToManyPropBuilder<M, E>.() -> Unit)?= null
    ) {
    }

    fun <E: Any> manyToMany(
        prop: KProperty1<M, List<E>>,
        propBuilderAction: (ManyToManyPropBuilder<M, E>.() -> Unit)?= null
    ) {

        fun mappedBy(inverseProp: KProperty1<E, List<M>>) {

        }
    }

    fun <T> computed(
        prop: KProperty1<M, T>,
        propBuilderAction: PropComputedConfigBuilder<M, T>.() -> ComputedExpression<T>
    ) {

    }

    inner class FilterBuilder internal constructor() {

        fun <T> match(prop: KProperty1<M, T>, value: T) {

        }
    }
}