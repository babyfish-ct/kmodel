package org.babyfish.kmodel.config

fun <M: Any> PropBuilder<M, *>.redis(
    action: RedisConfigBuilder<M>.() -> Unit
) {

}

fun <M: Any> ModelBuilder<M>.redis(
    action: RedisConfigBuilder<M>.() -> Unit
) {

}

class RedisConfigBuilder<M: Any> : ConfigBuilder<M>() {

    fun enabled(enabled: Boolean = true) {}

    fun keyPrefix(name: String) {}
}