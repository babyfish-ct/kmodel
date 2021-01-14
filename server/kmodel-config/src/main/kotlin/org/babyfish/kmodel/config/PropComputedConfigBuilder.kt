package org.babyfish.kmodel.config

import kotlin.reflect.KProperty1

class PropComputedConfigBuilder<M: Any, T> {

    fun <E, K, V, A: Accumulator<K, V>> aggregation(
        initializedAccumulator: Accumulator<K, V>,
        modelElementListProperty: KProperty1<M, List<E>>,
        elementKeyProperty: KProperty1<E, K>,
        elementValueProperty: KProperty1<E, V>
    ) =
        aggregation(
            initializedAccumulator,
            elementKeyProperty,
            elementValueProperty
        ) {
            listOf(modelElementListProperty)
        }

    fun <E, K, V, A: Accumulator<K, V>> aggregation(
        initializedAccumulator: Accumulator<K, V>,
        elementKeyProperty: KProperty1<E, K>,
        elementValueProperty: KProperty1<E, V>,
        modelElementListProperties: () -> List<KProperty1<M, List<E>>>
    ): ComputedExpression<V> {
        TODO()
    }
}

fun <M: Any, E: Any> PropComputedConfigBuilder<M, *>.count(
    modelElementListProperty: KProperty1<M, List<E>>
) : ComputedExpression<Long> = TODO()

fun <M: Any, E: Any, V: Number> PropComputedConfigBuilder<M, V>.sum(
    modelElementListProperty: KProperty1<M, List<E>>,
    elementValueProperty: KProperty1<E, V>
) : ComputedExpression<V> = TODO()

fun <M: Any, E: Any, V: Number> PropComputedConfigBuilder<M, V>.avg(
    modelElementListProperty: KProperty1<M, List<E>>,
    elementValueProperty: KProperty1<E, V>
) : ComputedExpression<V> = TODO()

fun <M: Any, E: Any, V: Comparable<V>> PropComputedConfigBuilder<M, V>.min(
    modelElementListProperty: KProperty1<M, List<E>>,
    elementValueProperty: KProperty1<E, V>
) : ComputedExpression<V> = TODO()

fun <M: Any, E: Any, V: Comparable<V>> PropComputedConfigBuilder<M, V>.max(
    modelElementListProperty: KProperty1<M, List<E>>,
    elementValueProperty: KProperty1<E, V>
) : ComputedExpression<V> = TODO()
