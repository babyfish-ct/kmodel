package org.babyfish.kmodel.test

import kotlin.test.DefaultAsserter
import kotlin.test.fail

class MapContext<K, V> internal constructor(
    parent: AbstractContext<*>?,
    parentProp: Any?,
    value: Map<K, V>?
): AbstractContext<Map<K, V>?>(
    parent,
    parentProp,
    value
) {
    fun size(size: Int) {
        val actual = value?.size ?: 0
        DefaultAsserter.assertTrue(
            lazyMessage = {
                "Illegal value of $this.size, Expected <$size>, actual <$actual>."
            },
            actual = size == actual
        )
    }

    fun value(key: K): ValueContext<V?> =
        value
            ?.let {
                ValueContext(
                    this,
                    key,
                    it[key]
                )
            }
            ?: fail("Cannot access $this by key because it's null")
}

fun <K, V> MapContext<K, V>.obj(
    key: K,
    contextAction: ObjectContext<V>.() -> Unit
) {
    value
        ?.let {
            ObjectContext(
                this,
                key,
                it[key] ?: fail("${member(key)} is null")
            ).contextAction()
        }
        ?: fail("Cannot access $this by key because it's null")
}

fun <K, E> MapContext<K, out Iterable<E>?>.list(
    key: K,
    contextAction: ListContext<E>.() -> Unit
) {
    value
        ?.let {
            ListContext(this, key, it[key]?.asList()).contextAction()
        }
        ?: fail("Cannot access $this by key because it's null")
}

fun <K, K2, V2> MapContext<K, out Map<K2, V2>>.map(
    key: K,
    contextAction: MapContext<K2, V2>.() -> Unit
) {
    value
        ?.let {
            MapContext(this, key, it[key]).contextAction()
        }
        ?: fail("Cannot access $this by key because it's null")
}