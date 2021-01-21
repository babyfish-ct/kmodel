package org.babyfish.kmodel.seata

import com.alibaba.druid.util.JdbcUtils
import org.babyfish.kmodel.jdbc.ConnectionProxy
import org.babyfish.kmodel.jdbc.DataChangedEvent
import org.babyfish.kmodel.jdbc.DeletionOption
import org.babyfish.kmodel.jdbc.metadata.ForeignKey
import java.sql.Connection

class ConnectionProxyForSeata(
    con: Connection,
    dataChangedListener: (DataChangedEvent) -> Unit,
    deletionOptionSupplier: ((ForeignKey) -> DeletionOption)?
) : ConnectionProxy(
    con,
    dataChangedListener,
    deletionOptionSupplier
) {

    init {
        val dbType = JdbcUtils.getDbType(
            con.metaData.url,
            con.metaData.driverName
        )
        hack(dbType)
    }
}
