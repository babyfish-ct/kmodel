package org.babyfish.kmodel.test

fun <O: Any> expectObj(
    obj: O,
    contextAction: ObjectContext<O>.() -> Unit
) {
    ObjectContext(null, null, obj)
        .contextAction()
}

fun <E> expectList(
    collection: Collection<E>,
    assertionAction: ListContext<E>.() -> Unit
) {
    ListContext(null, null, collection.asList())
        .assertionAction()
}

fun <K, V> expectMap(
    map: Map<K, V>,
    assertionAction: MapContext<K, V>.() -> Unit
) {
    MapContext(null, null, map)
        .assertionAction()
}

internal fun <E> Iterable<E>.asList(): List<E> =
    if (this is List<*>) {
        this as List<E>
    } else {
        toList()
    }