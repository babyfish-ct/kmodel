package org.babyfish.kmodel.jdbc

import org.babyfish.kmodel.jdbc.exec.Batch
import org.babyfish.kmodel.jdbc.exec.MutableParameters
import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.net.URL
import java.sql.*
import java.sql.Array
import java.sql.Date
import java.util.*

class PreparedStatementProxy internal constructor(
    proxyCon: ConnectionProxy,
    target: PreparedStatement,
    private val sql: String
) : StatementProxy(
    proxyCon,
    target
), PreparedStatement by target {

    internal val parameters = MutableParameters()

    init {
        (target as LazyProxy<PreparedStatement>).addTargetInitializedListener {
            for (paramIndex in 1 until parameters.setters.size) {
                parameters.setters[paramIndex]?.invoke(
                    it, paramIndex
                )
            }
        }
    }

    override val target: PreparedStatement
        get() = super.target as PreparedStatement

    override fun setArray(parameterIndex: Int, x: Array) {
        setParameter(parameterIndex, x) {
            setArray(it, x)
        }
    }

    override fun setAsciiStream(parameterIndex: Int, x: InputStream) {
        setParameter(parameterIndex, x) {
            setAsciiStream(it, x)
        }
    }

    override fun setAsciiStream(parameterIndex: Int, x: InputStream, length: Int) {
        setParameter(parameterIndex, x) {
            setAsciiStream(it, x, length)
        }
    }

    override fun setAsciiStream(parameterIndex: Int, x: InputStream, length: Long) {
        setParameter(parameterIndex, x) {
            setAsciiStream(it, x, length)
        }
    }

    override fun setBigDecimal(parameterIndex: Int, x: BigDecimal) {
        setParameter(parameterIndex, x) {
            setBigDecimal(it, x)
        }
    }

    override fun setBinaryStream(parameterIndex: Int, x: InputStream) {
        setParameter(parameterIndex, x) {
            setBinaryStream(it, x)
        }
    }

    override fun setBinaryStream(parameterIndex: Int, x: InputStream, length: Int) {
        setParameter(parameterIndex, x) {
            setBinaryStream(it, x, length)
        }
    }

    override fun setBinaryStream(parameterIndex: Int, x: InputStream, length: Long) {
        setParameter(parameterIndex, x) {
            setBinaryStream(it, x, length)
        }
    }

    override fun setBlob(parameterIndex: Int, inputStream: InputStream) {
        setParameter(parameterIndex, inputStream) {
            setBlob(it, inputStream)
        }
    }

    override fun setBlob(parameterIndex: Int, x: Blob) {
        setParameter(parameterIndex, x) {
            setBlob(it, x)
        }
    }

    override fun setBlob(parameterIndex: Int, inputStream: InputStream, length: Long) {
        setParameter(parameterIndex, inputStream) {
            setBlob(it, inputStream, length)
        }
    }

    override fun setBoolean(parameterIndex: Int, x: Boolean) {
        setParameter(parameterIndex, x) {
            setBoolean(it, x)
        }
    }

    override fun setByte(parameterIndex: Int, x: Byte) {
        setParameter(parameterIndex, x) {
            setByte(it, x)
        }
    }

    override fun setBytes(parameterIndex: Int, x: ByteArray) {
        setParameter(parameterIndex, x) {
            setBytes(it, x)
        }
    }

    override fun setCharacterStream(parameterIndex: Int, reader: Reader) {
        setParameter(parameterIndex, reader) {
            setCharacterStream(it, reader)
        }
    }

    override fun setCharacterStream(parameterIndex: Int, reader: Reader, length: Int) {
        setParameter(parameterIndex, reader) {
            setCharacterStream(it, reader, length)
        }
    }

    override fun setCharacterStream(parameterIndex: Int, reader: Reader, length: Long) {
        setParameter(parameterIndex, reader) {
            setCharacterStream(it, reader, length)
        }
    }

    override fun setClob(parameterIndex: Int, reader: Reader) {
        setParameter(parameterIndex, reader) {
            setClob(it, reader)
        }
    }

    override fun setClob(parameterIndex: Int, x: Clob) {
        setParameter(parameterIndex, x) {
            setClob(it, x)
        }
    }

    override fun setClob(parameterIndex: Int, reader: Reader, length: Long) {
        setParameter(parameterIndex, reader) {
            setClob(it, reader, length)
        }
    }

    override fun setDate(parameterIndex: Int, x: Date) {
        setParameter(parameterIndex, x) {
            setDate(it, x)
        }
    }

    override fun setDate(parameterIndex: Int, x: Date, cal: Calendar) {
        setParameter(parameterIndex, x) {
            setDate(it, x, cal)
        }
    }

    override fun setDouble(parameterIndex: Int, x: Double) {
        setParameter(parameterIndex, x) {
            setDouble(it, x)
        }
    }

    override fun setFloat(parameterIndex: Int, x: Float) {
        setParameter(parameterIndex, x) {
            setFloat(it, x)
        }
    }

    override fun setInt(parameterIndex: Int, x: Int) {
        setParameter(parameterIndex, x) {
            setInt(it, x)
        }
    }

    override fun setLong(parameterIndex: Int, x: Long) {
        setParameter(parameterIndex, x) {
            setLong(it, x)
        }
    }

    override fun setNCharacterStream(parameterIndex: Int, value: Reader) {
        setParameter(parameterIndex, value) {
            setNCharacterStream(it, value)
        }
    }

    override fun setNCharacterStream(parameterIndex: Int, value: Reader, length: Long) {
        setParameter(parameterIndex, value) {
            setNCharacterStream(it, value, length)
        }
    }

    override fun setNClob(parameterIndex: Int, reader: Reader) {
        setParameter(parameterIndex, reader) {
            setNClob(it, reader)
        }
    }

    override fun setNClob(parameterIndex: Int, value: NClob) {
        setParameter(parameterIndex, value) {
            setNClob(it, value)
        }
    }

    override fun setNClob(parameterIndex: Int, reader: Reader, length: Long) {
        setParameter(parameterIndex, reader) {
            setNClob(it, reader, length)
        }
    }

    override fun setNString(parameterIndex: Int, value: String) {
        setParameter(parameterIndex, value) {
            setNString(it, value)
        }
    }

    override fun setNull(parameterIndex: Int, sqlType: Int) {
        setParameter(parameterIndex, null) {
            setNull(it, sqlType)
        }
    }

    override fun setNull(parameterIndex: Int, sqlType: Int, typeName: String) {
        setParameter(parameterIndex, null) {
            setNull(it, sqlType, typeName)
        }
    }

    override fun setObject(parameterIndex: Int, x: Any) {
        setParameter(parameterIndex, x) {
            setObject(it, x)
        }
    }

    override fun setObject(parameterIndex: Int, x: Any, targetSqlType: Int) {
        setParameter(parameterIndex, x) {
            setObject(it, x, targetSqlType)
        }
    }

    override fun setObject(parameterIndex: Int, x: Any, targetSqlType: SQLType) {
        setParameter(parameterIndex, x) {
            setObject(it, x, targetSqlType)
        }
    }

    override fun setObject(parameterIndex: Int, x: Any, targetSqlType: Int, scaleOrLength: Int) {
        setParameter(parameterIndex, x) {
            setObject(it, targetSqlType, scaleOrLength)
        }
    }

    override fun setObject(parameterIndex: Int, x: Any, targetSqlType: SQLType, scaleOrLength: Int) {
        setParameter(parameterIndex, x) {
            setObject(it, x, targetSqlType, scaleOrLength)
        }
    }

    override fun setRef(parameterIndex: Int, x: Ref) {
        setParameter(parameterIndex, x) {
            setRef(it, x)
        }
    }

    override fun setRowId(parameterIndex: Int, x: RowId) {
        setParameter(parameterIndex, x) {
            setRowId(it, x)
        }
    }

    override fun setSQLXML(parameterIndex: Int, xmlObject: SQLXML) {
        setParameter(parameterIndex, xmlObject) {
            setSQLXML(it, xmlObject)
        }
    }

    override fun setShort(parameterIndex: Int, x: Short) {
        setParameter(parameterIndex, x) {
            setShort(it, x)
        }
    }

    override fun setString(parameterIndex: Int, x: String) {
        setParameter(parameterIndex, x) {
            setString(it, x)
        }
    }

    override fun setTime(parameterIndex: Int, x: Time) {
        setParameter(parameterIndex, x) {
            setTime(it, x)
        }
    }

    override fun setTime(parameterIndex: Int, x: Time, cal: Calendar) {
        setParameter(parameterIndex, x) {
            setTime(it, x)
        }
    }

    override fun setTimestamp(parameterIndex: Int, x: Timestamp) {
        setParameter(parameterIndex, x) {
            setTimestamp(it, x)
        }
    }

    override fun setTimestamp(parameterIndex: Int, x: Timestamp, cal: Calendar) {
        setParameter(parameterIndex, x) {
            setTimestamp(it, x, cal)
        }
    }

    override fun setURL(parameterIndex: Int, x: URL) {
        setParameter(parameterIndex, x) {
            setURL(it, x)
        }
    }

    override fun setUnicodeStream(parameterIndex: Int, x: InputStream, length: Int) {
        setParameter(parameterIndex, x) {
            setUnicodeStream(it, x, length)
        }
    }

    override fun execute(): Boolean =
        proxyCon.executionContext.execute(
            sql,
            parameters.toReadonly(),
            this
        )

    override fun executeUpdate(): Int =
        if (execute()) {
            -1
        } else {
            proxyCon.executionContext.getUpdateCount()
        }

    override fun executeLargeUpdate(): Long {
        return target.executeLargeUpdate()
    }

    override fun addBatch() {
        batches += Batch(
            sql = sql,
            parameters = parameters.toReadonly()
        )
    }

    // This override method looks very boring, but it's necessary
    //
    // kotlin the priority of kotlin delegate
    // is higher than the implementation of super class.
    override fun executeBatch(): IntArray =
        super.executeBatch()

    private fun setParameter(
            parameterIndex: Int,
            value: Any?,
            setter: PreparedStatement.(Int) -> Unit
    ) {
        if (parameters.setters.size <= parameterIndex) {
            for (i in parameters.values.size..parameterIndex + 1) {
                parameters.values += null as Any?
            }
            for (i in parameters.setters.size..parameterIndex + 1) {
                parameters.setters += null as (PreparedStatement.(Int) -> Unit)?
            }
        }
        parameters.values[parameterIndex] = value
        parameters.setters[parameterIndex] = setter
    }
}
