package org.babyfish.kmodel.config

import kotlin.reflect.KClass

open class PropBuilder<M: Any, T> {

    inline fun <reified B: PropConfigBuilder<M, T>> config(
        noinline propConfigBuilderAction: B.() -> Unit
    ) {
        config<B>(B::class, propConfigBuilderAction)
    }

    fun <B: PropConfigBuilder<M, T>> config(
        propConfigBuilderType: KClass<out PropConfigBuilder<*, *>>,
        propConfigBuilderAction: B.() -> Unit
    ) {
        TODO()
    }
}
