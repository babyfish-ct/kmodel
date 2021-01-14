package org.babyfish.kmodel.test

import kotlin.test.DefaultAsserter

class ValueContext<T>(
    parent: AbstractContext<*>,
    parentProp: Any?,
    value: T
): AbstractContext<T>(
    parent,
    parentProp,
    value
)

infix fun <T: Comparable<T>?> ValueContext<T>.lt(value: T) {
    DefaultAsserter.assertTrue(
        lazyMessage = {
            "Illegal reference of $this, expected value must be less than <$value>, actual <${this.value}>."
        },
        actual = this.value !== null && this.value < value
    )
}

fun <T:Comparable<T>?> ValueContext<T>.lt(valueSupplier: () -> T) {
    lt(valueSupplier())
}

infix fun <T: Comparable<T>?> ValueContext<T>.le(value: T) {
    DefaultAsserter.assertTrue(
        lazyMessage = {
            "Illegal reference of $this, expected value must be less than or equal to <$value>, actual <${this.value}>."
        },
        actual = this.value !== null && this.value <= value
    )
}

fun <T:Comparable<T>?> ValueContext<T>.le(valueSupplier: () -> T) {
    le(valueSupplier())
}

infix fun <T: Comparable<T>?> ValueContext<T>.gt(value: T) {
    DefaultAsserter.assertTrue(
        lazyMessage = {
            "Illegal reference of $this, expected value must be greater than <$value>, actual <${this.value}>."
        },
        actual = this.value !== null && this.value < value
    )
}

fun <T:Comparable<T>?> ValueContext<T>.gt(valueSupplier: () -> T) {
    gt(valueSupplier())
}

infix fun <T: Comparable<T>?> ValueContext<T>.ge(value: T) {
    DefaultAsserter.assertTrue(
        lazyMessage = {
            "Illegal reference of $this, expected value must be greater than or equal to <$value>, actual <${this.value}>."
        },
        actual = this.value !== null && this.value < value
    )
}

fun <T:Comparable<T>?> ValueContext<T>.ge(valueSupplier: () -> T) {
    ge(valueSupplier())
}