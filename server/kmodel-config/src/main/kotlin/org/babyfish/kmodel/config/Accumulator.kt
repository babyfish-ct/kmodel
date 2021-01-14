package org.babyfish.kmodel.config

interface Accumulator<K, V> {

    fun accumulate(key: K, value: V)

    fun calculate(): V
}