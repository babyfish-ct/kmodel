package org.babyfish.kmodel.config

import kotlin.reflect.KProperty1

class OneToManyPropBuilder<M: Any, E: Any> : PropBuilder<M, List<E>>() {

    fun mappedBy(inverseProp: KProperty1<E, M>) {

    }
}