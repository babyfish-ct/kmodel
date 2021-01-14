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
        }.let {
            expectObj(it) {
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
    }

    @Test
    fun testMultipleRowsByStatement() {}

    @Test
    fun testBatchByStatement() {}
}