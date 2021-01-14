package org.babyfish.kmodel.seata

import io.seata.rm.datasource.undo.SQLUndoLog

internal class UndoExecutorContext {

    private val _sqlUndoLogs = mutableListOf<SQLUndoLog>()

    val sqlUndoLogs: List<SQLUndoLog>
        get() = _sqlUndoLogs

    fun using(action: () -> Unit) {
        val oldContext = UNDO_EXECUTOR_CONTEXT_LOCAL.get()
        UNDO_EXECUTOR_CONTEXT_LOCAL.set(this)
        try {
            action()
        } finally {
            if (oldContext !== null) {
                UNDO_EXECUTOR_CONTEXT_LOCAL.set(oldContext)
            } else {
                UNDO_EXECUTOR_CONTEXT_LOCAL.remove()
            }
        }
    }

    fun append(sqlUndoLog: SQLUndoLog) {
        _sqlUndoLogs += sqlUndoLog
    }
}

private val UNDO_EXECUTOR_CONTEXT_LOCAL =
    ThreadLocal<UndoExecutorContext>()

internal fun currentUndoExecutorContext(): UndoExecutorContext =
    UNDO_EXECUTOR_CONTEXT_LOCAL.get()
        ?: error("There is no existing UndoContext")