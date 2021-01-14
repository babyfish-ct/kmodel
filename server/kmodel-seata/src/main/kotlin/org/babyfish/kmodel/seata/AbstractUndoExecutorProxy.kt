package org.babyfish.kmodel.seata

import io.seata.rm.datasource.sql.struct.TableRecords
import io.seata.rm.datasource.undo.AbstractUndoExecutor
import io.seata.rm.datasource.undo.SQLUndoLog
import java.sql.Connection

class AbstractUndoExecutorProxy(
    private val target: AbstractUndoExecutor
) : AbstractUndoExecutor(
    target.sqlUndoLog
) {
    override fun getSqlUndoLog(): SQLUndoLog {
        return target.sqlUndoLog
    }

    override fun executeOn(conn: Connection) {
        currentUndoExecutorContext().append(target.sqlUndoLog)
        target.executeOn(conn)
    }

    override fun getUndoRows(): TableRecords {
        error("Internal bug")
    }

    override fun buildUndoSQL(): String {
        error("Internal bug")
    }
}