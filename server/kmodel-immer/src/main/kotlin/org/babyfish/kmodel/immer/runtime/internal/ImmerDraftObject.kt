package org.babyfish.kmodel.immer.runtime.internal

interface ImmerDraft<T> {
    val isChanged: Boolean
    fun markChanged()
    fun toImmer(): T
}

interface ImmerDraftObject<T> : ImmerDraft<T>, ImmerObject {

    fun disableAll()

    fun disable(propName: String)
}