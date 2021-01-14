package org.babyfish.kmodel.seata

import io.seata.rm.datasource.undo.AbstractUndoExecutor
import io.seata.rm.datasource.undo.SQLUndoLog
import io.seata.rm.datasource.undo.UndoExecutorHolder

class UndoExecutorHolderProxy(
    private val target: UndoExecutorHolder
) : UndoExecutorHolder {

    override fun getDeleteExecutor(
        sqlUndoLog: SQLUndoLog
    ): AbstractUndoExecutor =
        AbstractUndoExecutorProxy(
            target.getDeleteExecutor(sqlUndoLog)
        )

    override fun getInsertExecutor(
        sqlUndoLog: SQLUndoLog
    ): AbstractUndoExecutor =
        AbstractUndoExecutorProxy(
            target.getInsertExecutor(sqlUndoLog)
        )

    override fun getUpdateExecutor(
        sqlUndoLog: SQLUndoLog
    ): AbstractUndoExecutor =
        AbstractUndoExecutorProxy(
            target.getUpdateExecutor(sqlUndoLog)
        )
}