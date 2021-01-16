package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.AbstractJdbcTest
import org.babyfish.kmodel.jdbc.metadata.QualifiedName
import java.math.BigDecimal
import java.sql.Connection

abstract class AbstractExecTest : AbstractJdbcTest() {

    override fun setupDatabase() {
        connection
            .createStatement()
            .apply {
                addBatch("""create table category(
                        |id long not null primary key,
                        |name varchar(20) not null
                        |)""".trimMargin())
                addBatch("""create table product(
                        |id long not null primary key, 
                        |name varchar(20) not null, 
                        |price number not null,
                        |category_id long not null,
                        |constraint fk_category 
                        |  foreign key(category_id) 
                        |    references category(id)
                        |)""".trimMargin()
                )
            }
            .executeBatch()
        connection
            .prepareStatement(
                "insert into category values(?, ?)"
            )
            .apply {
                setLong(1, 1)
                setString(2, "Food")
                addBatch()
                setLong(1, 2)
                setString(2, "Office")
                addBatch()
                executeBatch()
            }
            //.executeBatch()
        connection
            .prepareStatement(
                "insert into product values(?, ?, ?, ?)"
            ).apply {
                setLong(1, 1)
                setString(2, "Polk")
                setBigDecimal(3, BigDecimal("20"))
                setLong(4, 1)
                addBatch()
                setLong(1, 2)
                setString(2, "Beef")
                setBigDecimal(3, BigDecimal("30"))
                setLong(4, 1)
                addBatch()
                setLong(1, 3)
                setString(2, "Pen")
                setBigDecimal(3, BigDecimal("30"))
                setLong(4, 2)
                addBatch()
                setLong(1, 4)
                setString(2, "Pencil")
                setBigDecimal(3, BigDecimal("5"))
                setLong(4, 2)
                addBatch()
            }
            .executeBatch()
    }
}

internal val PRODUCT_QUALIFIED_NAME =
    QualifiedName(
        "INSERT-TEST",
        "PUBLIC",
        "PRODUCT"
    )