package org.babyfish.kmodel.jdbc.metadata

internal fun String.standardIdentifier(): String =
        when {
            startsWith("`") && endsWith("`") ->
                substring(1, length - 1)
            startsWith("\"") && endsWith("\"") ->
                substring(1, length - 1)
            startsWith("[") && endsWith("]") ->
                substring(1, length - 1)
            else ->
                this
        }
