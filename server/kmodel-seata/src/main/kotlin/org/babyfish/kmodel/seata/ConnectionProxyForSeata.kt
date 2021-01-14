package org.babyfish.kmodel.seata

import com.alibaba.druid.util.JdbcUtils
import org.babyfish.kmodel.jdbc.ConnectionProxy
import org.babyfish.kmodel.jdbc.DataChangedEvent
import java.sql.Connection

class ConnectionProxyForSeata(
    con: Connection,
    dataChangedListener: (DataChangedEvent) -> Unit
) : ConnectionProxy(con, dataChangedListener) {

    init {
        val dbType = JdbcUtils.getDbType(
            con.metaData.url,
            con.metaData.driverName
        )
        hack(dbType)
    }
}
