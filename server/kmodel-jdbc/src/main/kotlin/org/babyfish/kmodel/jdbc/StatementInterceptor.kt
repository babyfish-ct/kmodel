package org.babyfish.kmodel.jdbc

import java.sql.Statement

internal class StatementInterceptor {

    fun close(targetRef: LazyTargetRef<Statement>) {
        targetRef.get()?.close()
    }
}