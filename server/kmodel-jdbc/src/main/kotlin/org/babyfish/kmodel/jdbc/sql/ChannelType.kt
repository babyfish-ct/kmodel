package org.babyfish.kmodel.jdbc.sql

internal interface ChannelType {
    val preDependencyType: PreDependencyType
    val keywords: List<String>
}

internal enum class PreDependencyType {
    NONE,
    PREV_ALL,
    PREV_ONE
}