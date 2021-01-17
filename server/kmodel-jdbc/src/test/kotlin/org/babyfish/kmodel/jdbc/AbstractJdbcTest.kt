package org.babyfish.kmodel.jdbc

import org.babyfish.kmodel.jdbc.metadata.tableManager
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import java.lang.StringBuilder
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.fail

abstract class AbstractJdbcTest {

    private var _con: Connection? = null

    private var dataChangedEvent: DataChangedEvent? = null

    protected val connection: Connection
        get() = _con ?: error("connection can only be accessed during test")

    protected fun executeUpdate(
        action: Connection.() -> Unit
    ): DataChangedEvent {
        connection.autoCommit = false
        dataChangedEvent = null
        transaction {
            connection.action()
        }
        return dataChangedEvent ?: fail("No data changed event")
    }

    @Before
    fun initialize() {
        val dbNameBuilder = StringBuilder()
        var prevLowercase = false
        for (ch in this::class.simpleName!!) {
            val lowcase = ch.isLowerCase()
            if (prevLowercase && !lowcase) {
                dbNameBuilder.append('-')
            }
            dbNameBuilder.append(ch)
            prevLowercase = lowcase
        }
        val con = ConnectionProxy(
            DriverManager.getConnection("jdbc:h2:mem:${dbNameBuilder}", "sa", null)
        ) {
            dataChangedEvent = it
        }
        _con = con
        transaction {
            setupDatabase()
        }
    }

    @After
    fun uninitialize() {
        val con = _con
        con?.let {
            _con = null
            it.close()
        }
    }

    protected open fun setupDatabase() {}

    protected fun <R> transaction(
        action: () -> R
    ): R {
        connection.autoCommit = false
        val result = try {
            action()
        } catch (ex: Throwable) {
            connection.rollback()
            throw ex
        }
        connection.commit()
        return result
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun registerDriver() {
            Class.forName("org.h2.Driver")
        }
    }
}