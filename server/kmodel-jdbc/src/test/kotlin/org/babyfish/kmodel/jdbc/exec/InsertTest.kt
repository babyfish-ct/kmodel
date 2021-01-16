package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.DataChangedEvent
import org.babyfish.kmodel.test.asNonNull
import org.babyfish.kmodel.test.expectObj
import org.babyfish.kmodel.test.map
import org.babyfish.kmodel.test.obj
import org.junit.Test
import java.math.BigDecimal
import java.sql.Types
import kotlin.test.expect
import kotlin.test.fail

class InsertTest : AbstractExecTest() {

    @Test
    fun testSimpleByStatement() {
        executeUpdate {
            expect(1) {
                createStatement()
                    .executeUpdate(
                        """
                insert into product(name, id, category_id, price)
                    values('Tea', 5, 1, 25)"""
                    )
            }
        }.expectSingleRow()
    }

    @Test
    fun testMultipleRowsByStatement() {
        executeUpdate {
            expect(2) {
                createStatement()
                    .executeUpdate(
                        """
                insert into product 
                values
                (5, 'Tea', 25, 1),
                (6, 'Brush', 10, 2)"""
                    )
            }
        }.expectMultipleRows()
    }

    @Test
    fun testBatchByStatement() {
        executeUpdate {
            expect(listOf(1, 1)) {
                createStatement()
                    .apply {
                        addBatch(
                            """
                        insert into product 
                        values
                        (5, 'Tea', 25, 1)"""
                        )
                        addBatch(
                            """
                        insert into product 
                        values
                        (6, 'Brush', 10, 2)"""
                        )
                    }
                    .executeBatch()
                    .asList()
            }
        }.expectMultipleRows()
    }

    @Test
    fun testSimpleByPreparedStatement() {
        executeUpdate {
            expect(1) {
                prepareStatement(
                    """
                insert into product(name, id, category_id, price)
                values(?, ?, ?, ?)"""
                ).apply {
                    setNString(1, "Tea")
                    setShort(2, 5)
                    setInt(3, 1)
                    setFloat(4, 25F)
                }.executeUpdate()
            }
        }.expectSingleRow()
    }

    @Test
    fun testMultipleRowsByPreparedStatement() {
        executeUpdate {
            expect(2) {
                prepareStatement(
                    """
                insert into product 
                values (?, ?, ?, ?), (?, ?, ?, ?)"""
                ).apply {
                    setInt(1, 5)
                    setString(2, "Tea")
                    setDouble(3, 25.0)
                    setByte(4, 1)
                    setShort(5, 6)
                    setNString(6, "Brush")
                    setShort(7, 10)
                    setDouble(8, 2.0)
                }.executeUpdate()
            }
        }.expectMultipleRows()
    }

    @Test
    fun testBatchOnConflictKeyByStatement() {
        executeUpdate {
            expect(listOf(2, 2)) {
                createStatement()
                    .apply {
                        addBatch("""
                            insert into product(
                                price,
                                category_id,
                                id,
                                name
                            )
                            values
                            (25, 1, 5, 'Tea'),
                            (10, 2, 6, 'Brush')
                            on duplicated key 
                            update set
                            price = price + values(price),
                            name = excluded.name"""
                        )
                        addBatch("""
                            insert into product(
                                price,
                                category_id,
                                id,
                                name
                            )
                            values
                            (100, null, 3, 'new_Pen'),
                            (100, null, 4, 'new_Pencil')
                            on duplicated key 
                            update set
                            price = price + values(price),
                            name = excluded.name"""
                        )
                    }
                    .executeBatch()
                    .asList()
            }
        }.expectUpsertedMultipleRows()
    }

    @Test
    fun testInsertOnConflictKeyByPreparedStatement() {
        executeUpdate {
            expect(4) {
                prepareStatement("""
                    insert into product(
                        price,
                        category_id,
                        id,
                        name
                    )
                    values
                    (?, ?, ?, ?),
                    (?, ?, ?, ?),
                    (?, ?, ?, ?),
                    (?, ?, ?, ?)
                    on conflict(id) do update set
                    price = price + excluded.price,
                    name = values(name)"""
                ).apply {
                    setFloat(1, 100F)
                    setNull(2, Types.BIGINT)
                    setDouble(3, 3.0)
                    setString(4, "new_Pen")
                    setByte(5, 100)
                    setNull(6, Types.INTEGER)
                    setShort(7, 4)
                    setNString(8, "new_Pencil")
                    setDouble(9, 25.0)
                    setByte(10, 1)
                    setInt(11, 5)
                    setString(12, "Tea")
                    setShort(13, 10)
                    setDouble(14, 2.0)
                    setShort(15, 6)
                    setNString(16, "Brush")
                }.executeUpdate()
            }
        }.expectUpsertedMultipleRows()
    }

    @Test
    fun testInsertOnConflictKeyByStatement() {
        executeUpdate {
            expect(4) {
                createStatement()
                    .executeUpdate("""
                    insert into product(
                        price,
                        category_id,
                        id,
                        name
                    )
                    values
                    (100, null, 3, 'new_Pen'),
                    (100, null, 4, 'new_Pencil'),
                    (25, 1, 5, 'Tea'),
                    (10, 2, 6, 'Brush')
                    on duplicated key 
                    update set
                    price = price + values(price),
                    name = excluded.name"""
                    )
            }
        }.expectUpsertedMultipleRows()
    }

    @Test
    fun testOnConflictDoNothing() {
        executeUpdate {
            expect(1) {
                createStatement()
                    .executeUpdate(
                        """
                        insert into product(name, id, category_id, price)
                        values('Tea', 5, 1, 25), ('X', 1, 9999, 9999)
                        on conflict do nothing"""
                    )
            }
        }.expectSingleRow()
    }

    @Test
    fun testBatchOnConflictDoNothing() {
        executeUpdate {
            expect(listOf(1, 0)) {
                createStatement()
                    .apply {
                        addBatch(
                            """
                            insert into product(name, id, category_id, price)
                            values('Tea', 5, 1, 25)
                            on conflict do nothing"""
                        )
                        addBatch(
                            """
                            insert into product(name, id, category_id, price)
                            values('X', 1, 9999, 9999)
                            on conflict do nothing"""
                        )
                    }
                    .executeBatch()
                    .asList()
            }
        }.expectSingleRow()
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

    private fun DataChangedEvent.expectUpsertedMultipleRows() {
        expectObj(this) {
            map(DataChangedEvent::beforeImageMap) {
                size(1)
                map(PRODUCT_QUALIFIED_NAME) {
                    size(4)
                    obj(listOf(3L)) {
                        asNonNull {
                            list(Row::pkValues) {
                                size(1)
                                value(0) eq 3L
                            }
                            map(Row::otherValueMap) {
                                size(3)
                                value("NAME") eq "Pen"
                                value("PRICE") eq BigDecimal("30")
                                value("CATEGORY_ID") eq 2L
                            }
                        }
                    }
                    obj(listOf(4L)) {
                        asNonNull {
                            list(Row::pkValues) {
                                size(1)
                                value(0) eq 4L
                            }
                            map(Row::otherValueMap) {
                                size(3)
                                value("NAME") eq "Pencil"
                                value("PRICE") eq BigDecimal("5")
                                value("CATEGORY_ID") eq 2L
                            }
                        }
                    }
                    value(listOf(5L)) same null
                    value(listOf(6L)) same null
                }
            }
            map (DataChangedEvent::afterImageMap) {
                size(1)
                map(PRODUCT_QUALIFIED_NAME) {
                    size(4)
                    obj(listOf(3L)) {
                        list(Row::pkValues) {
                            size(1)
                            value(0) eq 3L
                        }
                        map(Row::otherValueMap) {
                            size(3)
                            value("NAME") eq "new_Pen"
                            value("PRICE") eq BigDecimal("130")
                            value("CATEGORY_ID") eq 2L
                        }
                    }
                    obj(listOf(4L)) {
                        list(Row::pkValues) {
                            size(1)
                            value(0) eq 4L
                        }
                        map(Row::otherValueMap) {
                            size(3)
                            value("NAME") eq "new_Pencil"
                            value("PRICE") eq BigDecimal("105")
                            value("CATEGORY_ID") eq 2L
                        }
                    }
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