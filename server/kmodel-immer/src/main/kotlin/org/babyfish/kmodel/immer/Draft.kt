package org.babyfish.kmodel.immer

import kotlin.reflect.KProperty1

interface Draft

fun <D: Draft> isDisabled(draft: D, property: KProperty1<D, *>): Boolean =
    TODO()

fun disableAll(draft: Draft) {

}

fun <D: Draft> disable(draft: D, property: KProperty1<D, *>) {

}