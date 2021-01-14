package org.babyfish.kmodel.config

class ToOnePropBuilder<M: Any, R: Any> : PropBuilder<M, R>() {

    fun onDeleteSetNull() {}

    fun onDeleteCascade() {}
}