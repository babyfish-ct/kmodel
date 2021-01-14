package org.babyfish.kmodel.config

fun <M: Any, T> PropBuilder<M, T>.graphql(
    action: PropGraphQLConfigBuilder<M, T>.() -> Unit
) {
    this.config(action)
}

class PropGraphQLConfigBuilder<M: Any, T> : PropConfigBuilder<M, T>() {

    fun batchSize(size: Int) {}
}