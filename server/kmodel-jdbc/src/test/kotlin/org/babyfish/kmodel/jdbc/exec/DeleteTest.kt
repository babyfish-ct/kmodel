package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.DataChangedEvent
import org.babyfish.kmodel.jdbc.metadata.QualifiedName
import org.babyfish.kmodel.test.toNonNull
import org.babyfish.kmodel.test.expectObj
import org.babyfish.kmodel.test.map
import org.babyfish.kmodel.test.obj
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.expect

class DeleteTest : AbstractSimpleTest() {

    @Test
    fun testDeleteByStatement() {
        executeUpdate {
            expect(2) {
                createStatement()
                    .executeUpdate("""
                        |delete from product as p
                        |inner join category as c on p.category_id = c.id
                        |where c.name = 'Food'""".trimMargin())
            }
        }.expectDeletedRows()
    }

    @Test
    fun testBatchDeleteByStatement() {
        executeUpdate {
            expect(listOf(1, 1)) {
                createStatement()
                    .apply {
                        addBatch("delete from product p where p.id = 1")
                        addBatch("delete from product p where p.id = 2")
                    }
                    .executeBatch()
                    .asList()
            }
        }.expectDeletedRows()
    }

    @Test
    fun testDeleteByPreparedStatement() {
        executeUpdate {
            expect(2) {
                prepareStatement("""
                    |delete from product as p
                    |inner join category as c on p.category_id = c.id
                    |where c.name = ?""".trimMargin()
                ).apply {
                    setString(1, "Food")
                }.executeUpdate()
            }
        }.expectDeletedRows()
    }

    @Test
    fun testBatchDeleteByPreparedStatement() {
        executeUpdate {
            expect(listOf(1, 1)) {
                prepareStatement("delete from product p where p.id = ?")
                    .apply {
                        setBigDecimal(1, BigDecimal.ONE)
                        addBatch()
                        setFloat(1, 2F)
                        addBatch()
                    }
                    .executeBatch()
                    .asList()
            }
        }.expectDeletedRows()
    }

    private fun DataChangedEvent.expectDeletedRows() {
        expectObj(this) {
            map(DataChangedEvent::beforeImageMap) {
                size(1)
                map(
                    QualifiedName(
                        "DELETE-TEST",
                        "PUBLIC",
                        "PRODUCT"
                    )
                ) {
                    size(2)
                    obj(listOf(1L)) {
                        toNonNull {
                            value(Row::pkValues) eq listOf(1L)
                            value(Row::otherValueMap) eq mapOf(
                                "NAME" to "Polk",
                                "PRICE" to BigDecimal("20"),
                                "CATEGORY_ID" to 1L
                            )
                        }
                    }
                    obj(listOf(2L)) {
                        toNonNull {
                            value(Row::pkValues) eq listOf(2L)
                            value(Row::otherValueMap) eq mapOf(
                                "NAME" to "Beef",
                                "PRICE" to BigDecimal(30),
                                "CATEGORY_ID" to 1L
                            )
                        }
                    }
                }
            }
            map(DataChangedEvent::afterImageMap) {
                size(1)
                map(
                    QualifiedName(
                        "DELETE-TEST",
                        "PUBLIC",
                        "PRODUCT"
                    )
                ) {
                    size(0)
                }
            }
        }
    }
}