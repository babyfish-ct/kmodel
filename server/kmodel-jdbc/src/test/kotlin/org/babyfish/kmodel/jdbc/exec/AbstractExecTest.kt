package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.AbstractJdbcTest
import org.babyfish.kmodel.jdbc.metadata.QualifiedName
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
                executeBatch()
                addBatch("insert into category values(1, 'Food')")
                addBatch("insert into category values(2, 'Office')")
                addBatch("insert into product values(1, 'Polk', 20, 1)")
                addBatch("insert into product values(2, 'Beef', 30, 1)")
                addBatch("insert into product values(3, 'Pen', 30, 2)")
                addBatch("insert into product values(4, 'Pencil', 5, 2)")
                executeBatch()
            }
    }
}

internal val PRODUCT_QUALIFIED_NAME =
    QualifiedName(
        "INSERT-TEST",
        "PUBLIC",
        "PRODUCT"
    )