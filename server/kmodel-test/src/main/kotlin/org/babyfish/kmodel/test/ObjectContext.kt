package org.babyfish.kmodel.test

import kotlin.reflect.KProperty1
import kotlin.test.fail

class ObjectContext<O> internal constructor(
    parent: AbstractContext<*>?,
    parentProp: Any?,
    value: O
): AbstractContext<O>(
    parent,
    parentProp,
    value
) {
    fun <T> value(prop: KProperty1<O, T>): ValueContext<T> =
        ValueContext(
            this,
            prop,
            value?.let {
                prop.get(it)
            } as T
        )

    fun <T> obj(
        prop: KProperty1<O, T>,
        contextAction: ObjectContext<T>.() -> Unit
    ) {
        ObjectContext(
            this,
            prop,
            value?.let {
                if (it !== null) {
                    prop.get(it)
                } else {
                    null
                }
            } as T
        ).contextAction()
    }

    fun <E> list(
        prop: KProperty1<O, out Iterable<E>?>,
        contextAction: ListContext<E>.() -> Unit
    ) {
        ListContext(
            this,
            prop,
            value
                ?.let {
                    if (it !== null) {
                        prop.get(it)
                    } else {
                        null
                    }
                }?.asList()
        ).contextAction()
    }

    fun <K, V> map(
        prop: KProperty1<O, out Map<K, V>?>,
        contextAction: MapContext<K, V>.() -> Unit
    ) {
        MapContext(
            this,
            prop,
            value
                ?.let {
                    if (it !== null) {
                        prop.get(it)
                    } else {
                        null
                    }
                }
        ).contextAction()
    }
}

inline fun <O> ObjectContext<O?>.toNonNull(
    contextAction: ObjectContext<O>.() -> Unit
) {
    (this as ObjectContext<O>).contextAction()
}
