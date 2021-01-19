package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.AbstractJdbcTest
import org.junit.Test
import java.math.BigDecimal
import java.sql.Types
import kotlin.test.Ignore

class CascadeDeleteTest : AbstractJdbcTest() {

    override fun setupDatabase() {
        transaction {
            connection
                .createStatement()
                .apply {
                    addBatch(
                        """
                        create table department(
                            id bigint not null,
                            name varchar(50) not null,
                            constraint pk_department 
                                primary key(id)
                        )
                        """.trimIndent()
                    )
                    addBatch("""
                        create table employee(
                            id bigint not null,
                            name varchar(50) not null,
                            salary number not null,
                            department_id bigint not null,
                            supervisor_id bigint,
                            constraint pk_employee 
                                primary key(id),
                            constraint fk_employee_department 
                                foreign key(department_id)
                                    references department(id),
                            constraint fk_employee_supervisor 
                                foreign key(supervisor_id)
                                    references employee(id)
                        )"""
                    )
                }
                .executeBatch()
            connection
                .prepareStatement("""
                    insert into department(id, name)
                    values(?, ?)
                    """.trimIndent()
                )
                .apply {
                    setLong(1, 1L)
                    setString(2, "Product")
                    addBatch()
                    setByte(1, 2)
                    setNString(2, "Develop")
                    addBatch()
                    setBigDecimal(1, BigDecimal("3"))
                    setString(2, "Test")
                    addBatch()
                }
                .executeBatch()
            connection
                .prepareStatement("""
                    insert into employee(id, name, salary, department_id, supervisor_id)
                    values(?, ?, ?, ?, ?)
                    """.trimIndent()
                )
                .apply {
                    setString(1, "1")
                    setString(2, "Carlson")
                    setString(3, "100000")
                    setFloat(4, 1F)
                    setNull(5, Types.SMALLINT)
                    addBatch()
                    children {
                        setString(1, "2")
                        setString(2, "Jessica")
                        setString(3, "60000")
                        setFloat(4, 1F)
                        setShort(5, 1)
                        addBatch()
                        children {
                            setString(1, "3")
                            setString(2, "Van")
                            setString(3, "30000")
                            setFloat(4, 1F)
                            setShort(5, 2)
                            addBatch()
                            setString(1, "4")
                            setString(2, "Sarah")
                            setString(3, "30000")
                            setFloat(4, 1F)
                            setShort(5, 2)
                            addBatch()
                        }
                        setString(1, "5")
                        setString(2, "Morton")
                        setString(3, "60000")
                        setFloat(4, 1F)
                        setShort(5, 1)
                        addBatch()
                        children {
                            setString(1, "6")
                            setString(2, "Ellie")
                            setString(3, "30000")
                            setFloat(4, 1F)
                            setShort(5, 5)
                            addBatch()
                            setString(1, "7")
                            setString(2, "Quinton")
                            setString(3, "30000")
                            setFloat(4, 1F)
                            setShort(5, 5)
                            addBatch()
                        }
                    }
                }
                .executeBatch()
        }
    }

    @Ignore
    @Test
    fun test() {
        connection
            .prepareStatement("""
                delete from department where id = ?
                """.trimIndent()
            )
            .apply {
                setLong(1, 1)
            }
            .executeUpdate()
    }
}

private inline fun <R> children(action: () -> R): R =
    action()