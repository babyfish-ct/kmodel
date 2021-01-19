package org.babyfish.kmodel.jdbc

enum class ForeignKeyBehavior {
    NONE,
    UPDATE_SET_NULL,
    DELETE_CASCADE
}