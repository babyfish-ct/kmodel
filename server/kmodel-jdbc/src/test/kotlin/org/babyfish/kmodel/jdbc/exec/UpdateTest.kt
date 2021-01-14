package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.AbstractJdbcTest
import org.babyfish.kmodel.jdbc.ConnectionProxy
import org.babyfish.kmodel.jdbc.metadata.TableManager
import org.babyfish.kmodel.jdbc.metadata.tableManager
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import kotlin.test.expect

class UpdateTest: AbstractExecTest() {

    @Test
    fun test() {
        transaction {
            val con = connection!!
            val updateCount = con
                .prepareStatement(
                    """update product p
                            |set p.price = p.price + ?
                            |where p.id = ?""".trimMargin()
                )
                .using {
                    it.setBigDecimal(1, BigDecimal.ONE)
                    it.setLong(2, 1)
                    it.executeUpdate()
                }
            expect(1) {
                updateCount
            }
            con.prepareStatement(
                "insert into product(name, price, category_id, id) " +
                        "values(?, ?, ?, ?), (?, ?, ?, ?) " +
                        "on duplicate key " +
                        "update price = values(price) + 3"
            ).using {
                it.setString(1, "XBox")
                it.setBigDecimal(2, BigDecimal.TEN)
                it.setLong(3, 1)
                it.setLong(4, 1)
                it.setString(5, "PS2")
                it.setBigDecimal(6, BigDecimal.TEN)
                it.setLong(7, 1)
                it.setLong(8, 3)
                expect(2) {
                    it.executeUpdate()
                }
            }
        }
    }
}