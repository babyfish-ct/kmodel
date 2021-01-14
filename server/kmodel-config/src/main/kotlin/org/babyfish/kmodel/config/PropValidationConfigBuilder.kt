package org.babyfish.kmodel.config

fun <M: Any, T> PropBuilder<M, T>.validation(
    action: PropValidationConfigBuilder<M, T>.() -> Unit
) {
    this.config(action)
}

class PropValidationConfigBuilder<M: Any, T> : PropConfigBuilder<M, T>()

fun <M: Any> PropValidationConfigBuilder<M, String>.minLength(minLength: Int) {

}

fun <M: Any> PropValidationConfigBuilder<M, String>.maxLength(maxLength: Int) {

}

fun <M: Any, T: Number> PropValidationConfigBuilder<M, T>.minValue(minValue: Int, inclusive: Boolean = true) {

}

fun <M: Any, T: Number> PropValidationConfigBuilder<M, T>.maxValue(maxValue: Int, inclusive: Boolean = true) {

}