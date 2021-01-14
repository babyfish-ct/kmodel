package org.babyfish.kmodel.seata

import io.seata.rm.datasource.undo.SQLUndoLog
import io.seata.rm.datasource.undo.UndoLogManager

typealias SeataDataSourceProxy = io.seata.rm.datasource.DataSourceProxy
typealias SeataConnectionProxy = io.seata.rm.datasource.ConnectionProxy

class UndoLogManagerProxy(
    private val target: UndoLogManager
) : UndoLogManager by target {

    override fun flushUndoLogs(cp: SeataConnectionProxy) {
        target.flushUndoLogs(cp)
    }

    override fun undo(
        dataSourceProxy: SeataDataSourceProxy,
        xid: String,
        branchId: Long
    ) {
        UndoExecutorContext().using {
            target.undo(dataSourceProxy, xid, branchId)
        }
    }
}

