package org.babyfish.kmodel.immer.runtime.internal

import org.babyfish.kmodel.immer.runtime.PropertyState

interface ImmerObject {

    fun propertyStateOrdinal(propName: String): Int

    fun propertyState(propName: String): PropertyState =
        STATES[propertyStateOrdinal(propName)]
}

private val STATES = PropertyState.values()