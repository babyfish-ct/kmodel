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

class UpdateTest: AbstractExecTest() {

    @Test
    fun testUpdateByStatement() {
        executeUpdate {
            expect(2) {
                connection
                    .createStatement()
                    .executeUpdate(
                        """update product as p
                            |set p.price = p.price + 1 
                            |where p.category_id = 1""".trimMargin()
                    )
            }
        }.expectUpdatedFoods()
    }

    @Test
    fun testBatchUpdateByStatement() {
        executeUpdate {
            expect(listOf(1, 1)) {
                connection
                    .createStatement()
                    .apply {
                        addBatch(
                            "update product as p " +
                                    "set p.price = p.price + 1 where p.id = 1"
                        )
                        addBatch(
                            "update product as p " +
                                    "set p.price = p.price + 1 where p.id = 2"
                        )
                    }
                    .executeBatch()
                    .asList()
            }
        }.expectUpdatedFoods()
    }

    @Test
    fun testUpdateByPreparedStatement() {
        executeUpdate {
            expect(2) {
                connection
                    .prepareStatement(
                        """update product as p
                            |set p.price = p.price + ? 
                            |where p.category_id = ?""".trimMargin()
                    ).apply {
                        setBigDecimal(1, BigDecimal.ONE)
                        setFloat(2, 1.0F)
                    }
                    .executeUpdate()
            }
        }.expectUpdatedFoods()
    }

    @Test
    fun testBatchUpdateByPreparedStatement() {
        executeUpdate {
            expect(listOf(1, 1)) {
                connection
                    .prepareStatement(
                        "update product as p " +
                                "set p.price = p.price + ? where p.id = ?"
                    )
                    .apply {
                        setBigDecimal(1, BigDecimal.ONE)
                        setByte(2, 1)
                        addBatch()
                        setBigDecimal(1, BigDecimal.ONE)
                        setByte(2, 2)
                        addBatch()
                    }
                    .executeBatch()
                    .asList()
            }
        }.expectUpdatedFoods()
    }

    private fun DataChangedEvent.expectUpdatedFoods() {
        expectObj(this) {
            map(DataChangedEvent::beforeImageMap) {
                size(1)
                map(
                    QualifiedName(
                        "UPDATE-TEST",
                        "PUBLIC",
                        "PRODUCT"
                    )
                ) {
                    size(2)
                    obj(listOf(1L)) {
                        toNonNull {
                            list(Row::pkValues) {
                                size(1)
                                value(0) eq 1L
                            }
                            map(Row::otherValueMap) {
                                size(1)
                                value("PRICE") eq BigDecimal("20")
                            }
                        }
                    }
                    obj(listOf(2L)) {
                        toNonNull {
                            list(Row::pkValues) {
                                size(1)
                                value(0) eq 2L
                            }
                            map(Row::otherValueMap) {
                                size(1)
                                value("PRICE") eq BigDecimal("30")
                            }
                        }
                    }
                }
            }
            map(DataChangedEvent::afterImageMap) {
                size(1)
                map(
                    QualifiedName(
                        "UPDATE-TEST",
                        "PUBLIC",
                        "PRODUCT"
                    )
                ) {
                    size(2)
                    obj(listOf(1L)) {
                        list(Row::pkValues) {
                            size(1)
                            value(0) eq 1L
                        }
                        map(Row::otherValueMap) {
                            size(3)
                            value("NAME") eq "Polk"
                            value("PRICE") eq BigDecimal("21")
                            value("CATEGORY_ID") eq 1L
                        }
                    }
                    obj(listOf(2L)) {
                        list(Row::pkValues) {
                            size(1)
                            value(0) eq 2L
                        }
                        map(Row::otherValueMap) {
                            size(3)
                            value("NAME") eq "Beef"
                            value("PRICE") eq BigDecimal("31")
                            value("CATEGORY_ID") eq 1L
                        }
                    }
                }
            }
        }
    }
}