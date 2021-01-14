package org.babyfish.kmodel.test

import kotlin.test.DefaultAsserter
import kotlin.test.fail

class ListContext<E> internal constructor(
    parent: AbstractContext<*>?,
    parentProp: Any?,
    value: List<E>?
): AbstractContext<List<E>?>(
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

    fun value(index: Int): ValueContext<E> {
        if (value === null) {
            fail("Cannot access $this by index because it's null")
        }
        if (index < 0 || index >= value.size) {
            fail("illegal index for $this")
        }
        return ValueContext(this, index, value[index])
    }
}

fun <E> ListContext<E>.obj(
    index: Int,
    contextAction: ObjectContext<E>.() -> Unit
) {
    if (value === null) {
        fail("Cannot access $this by index because it's null")
    }
    if (index < 0 || index >= value.size) {
        fail("illegal index for $this")
    }
    ObjectContext(this, index, value[index]).contextAction()
}

fun <E> ListContext<out Iterable<E>?>.list(
    index: Int,
    contextAction: ListContext<E>.() -> Unit
) {
    if (value === null) {
        fail("Cannot access $this by index because it's null")
    }
    if (index < 0 || index >= value.size) {
        fail("illegal index for $this")
    }
    ListContext(this, index, value[index]?.asList()).contextAction()
}

fun <K, V> ListContext<out Map<K, V>?>.map(
    index: Int,
    contextAction: MapContext<K, V>.() -> Unit
) {
    if (value === null) {
        fail("Cannot access $this by index because it's null")
    }
    if (index < 0 || index >= value.size) {
        fail("illegal index for $this")
    }
    MapContext(this, index, value[index]).contextAction()
}