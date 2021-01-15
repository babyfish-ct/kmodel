package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.DataChangedEvent
import org.babyfish.kmodel.test.expectObj
import org.babyfish.kmodel.test.map
import org.babyfish.kmodel.test.obj
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.fail

class InsertTest : AbstractExecTest() {

    @Test
    fun testSimpleByStatement() {
        executeUpdate {
            createStatement()
            .executeUpdate(
                """
                insert into product(name, id, category_id, price)
                    values('Tea', 5, 1, 25)"""
            )
        }.expectSingleRow()
    }

    @Test
    fun testMultipleRowsByStatement() {
        executeUpdate {
            createStatement()
            .executeUpdate("""
                insert into product 
                values
                (5, 'Tea', 25, 1),
                (6, 'Brush', 10, 2)"""
            )
        }.expectMultipleRows()
    }

    @Test
    fun testBatchByStatement() {
        executeUpdate {
            createStatement()
                .apply {
                    addBatch("""
                        insert into product 
                        values
                        (5, 'Tea', 25, 1)"""
                    )
                    addBatch("""
                        insert into product 
                        values
                        (6, 'Brush', 10, 2)"""
                    )
                    executeBatch()
                }
        }.expectMultipleRows()
    }

    private fun DataChangedEvent.expectSingleRow() {
        expectObj(this) {
            map(DataChangedEvent::beforeImageMap) {
                size(1)
                map(PRODUCT_QUALIFIED_NAME) {
                    size(1)
                    value(listOf(5L)) same null
                }
            }
            map (DataChangedEvent::afterImageMap) {
                size(1)
                map(PRODUCT_QUALIFIED_NAME) {
                    size(1)
                    obj(listOf(5L)) {
                        list(Row::pkValues) {
                            size(1)
                            value(0) eq 5L
                        }
                        map(Row::otherValueMap) {
                            size(3)
                            value("NAME") eq "Tea"
                            value("PRICE") eq BigDecimal("25")
                            value("CATEGORY_ID") eq 1L
                        }
                    }
                }
            }
        }
    }

    private fun DataChangedEvent.expectMultipleRows() {
        expectObj(this) {
            map(DataChangedEvent::beforeImageMap) {
                size(1)
                map(PRODUCT_QUALIFIED_NAME) {
                    size(2)
                    value(listOf(5L)) same null
                    value(listOf(6L)) same null
                }
            }
            map (DataChangedEvent::afterImageMap) {
                size(1)
                map(PRODUCT_QUALIFIED_NAME) {
                    size(2)
                    obj(listOf(5L)) {
                        list(Row::pkValues) {
                            size(1)
                            value(0) eq 5L
                        }
                        map(Row::otherValueMap) {
                            size(3)
                            value("NAME") eq "Tea"
                            value("PRICE") eq BigDecimal("25")
                            value("CATEGORY_ID") eq 1L
                        }
                    }
                    obj(listOf(6L)) {
                        list(Row::pkValues) {
                            size(1)
                            value(0) eq 6L
                        }
                        map(Row::otherValueMap) {
                            size(3)
                            value("NAME") eq "Brush"
                            value("PRICE") eq BigDecimal("10")
                            value("CATEGORY_ID") eq 2L
                        }
                    }
                }
            }
        }
    }
}