package org.babyfish.kmodel.config

class ModelGraphQLBuilder<M: Any> {

    fun batchSize(size: Int) {

    }
}

fun <M: Any> ModelBuilder<M>.graphql(
    graphQLBuilderAction: ModelGraphQLBuilder<M>.() -> Unit
) {

}