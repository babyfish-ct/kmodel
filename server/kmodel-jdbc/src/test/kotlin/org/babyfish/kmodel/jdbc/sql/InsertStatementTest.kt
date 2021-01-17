package org.babyfish.kmodel.jdbc.sql

import org.babyfish.kmodel.test.expectObj
import org.babyfish.kmodel.test.obj
import org.junit.Test
import java.sql.SQLException
import kotlin.test.*

class InsertStatementTest {

    @Test
    fun testInsert() {
        val stmt = parseSqlStatement<InsertStatement>(
                "INSERT INTO PRODUCT VALUES(1, 'Pen', 3.0)"
        )
        expectObj(stmt) {
            obj(InsertStatement::tableClauseRange) {
                rangeEq("PRODUCT", 1, 0, stmt)
            }
            list(InsertStatement::insertedColumnRanges) {
                size(0)
            }
            list(InsertStatement::rows) {
                size(1)
                obj(0) {
                    list(InsertStatement.Row::values) {
                        size(3)
                        obj(0) {
                            rangeEq("1", 1, 0, stmt)
                        }
                        obj(1) {
                            rangeEq("'Pen'", 1, 0, stmt)
                        }
                        obj(2) {
                            rangeEq("3.0", 1, 0, stmt)
                        }
                    }
                }
            }
            value(InsertStatement::conflictPolicy) same null
        }
    }

    @Test
    fun testInsertColumnsByMultiRows() {
        val stmt = parseSqlStatement<InsertStatement>(
                "INSERT INTO PRODUCT(ID, \"NAME\", [PRICE])" +
                        "VALUES(1, 'Pen', 3.0), (2, 'Pencil', 1.0), (?, ?, ?)"
        )
        expectObj(stmt) {
            obj(InsertStatement::tableClauseRange) {
                rangeEq("PRODUCT", 1, 0, stmt)
            }
            list(InsertStatement::insertedColumnRanges) {
                size(3)
                obj(0) {
                    rangeEq("ID", 1, 0, stmt)
                }
                obj(1) {
                    rangeEq("\"NAME\"", 1, 0, stmt)
                }
                obj(2) {
                    rangeEq("[PRICE]", 1, 0, stmt)
                }
            }
            list(InsertStatement::rows) {
                size(3)
                obj(0) {
                    list(InsertStatement.Row::values) {
                        size(3)
                        obj(0) {
                            rangeEq("1", 1, 0, stmt)
                        }
                        obj(1) {
                            rangeEq("'Pen'", 1, 0, stmt)
                        }
                        obj(2) {
                            rangeEq("3.0", 1, 0, stmt)
                        }
                    }
                }
                obj(1) {
                    list(InsertStatement.Row::values) {
                        size(3)
                        obj(0) {
                            rangeEq("2", 1, 0, stmt)
                        }
                        obj(1) {
                            rangeEq("'Pencil'", 1, 0, stmt)
                        }
                        obj(2) {
                            rangeEq("1.0", 1, 0, stmt)
                        }
                    }
                }
                obj(2) {
                    list(InsertStatement.Row::values) {
                        size(3)
                        obj(0) {
                            rangeEq("?", 1, 1, stmt)
                        }
                        obj(1) {
                            rangeEq("?", 2, 1, stmt)
                        }
                        obj(2) {
                            rangeEq("?", 3, 1, stmt)
                        }
                    }
                }
            }
            value(InsertStatement::conflictPolicy) same null
        }
    }

    @Test
    fun testInsertOnConflictDoNoting() {
        val stmt = parseSqlStatement<InsertStatement>(
                "INSERT INTO PRODUCT VALUES(1, 'Pen', 3.0)" +
                        "ON CONFLICT(ID) DO NOTHING"
        )
        stmt.expectTokenRange("PRODUCT", 1, 0) {
            stmt.tableClauseRange
        }
        assertTrue {
            stmt.insertedColumnRanges.isEmpty()
        }
        expect(1) {
            stmt.rows.size
        }
        stmt.expectTokenRange("1", 1, 0) {
            stmt.rows[0].values[0]
        }
        stmt.expectTokenRange("'Pen'", 1, 0) {
            stmt.rows[0].values[1]
        }
        stmt.expectTokenRange("3.0", 1, 0) {
            stmt.rows[0].values[2]
        }

        val conflictPolicy = stmt.conflictPolicy ?: fail()
        expect("ID") {
            conflictPolicy.columnNames.joinToString { it }
        }
        assertNull(conflictPolicy.constraintName)
        assertTrue {
            conflictPolicy.updatedActions.isEmpty()
        }
    }

    @Test
    fun testInsertOnConflictDoUpdate() {
        val stmt = parseSqlStatement<InsertStatement>(
                "INSERT INTO PRODUCT(ID, \"NAME\", [PRICE])" +
                        "VALUES(1, 'Pen', 3.0), (2, 'Pencil', 1.0), (?, ?, ?)" +
                        "ON CONFLICT(ID, NAME) DO UPDATE SET " +
                        "PRICE = EXCLUDED.PRICE"
        )
        stmt.expectTokenRange("PRODUCT", 1, 0) {
            stmt.tableClauseRange
        }
        expect("ID, \"NAME\", [PRICE]") {
            stmt.insertedColumnRanges.joinToString {
                stmt.tokens[it.fromIndex].text
            }
        }
        expect(3) {
            stmt.rows.size
        }
        stmt.expectTokenRange("1", 1, 0) {
            stmt.rows[0].values[0]
        }
        stmt.expectTokenRange("'Pen'", 1, 0) {
            stmt.rows[0].values[1]
        }
        stmt.expectTokenRange("3.0", 1, 0) {
            stmt.rows[0].values[2]
        }
        stmt.expectTokenRange("2", 1, 0) {
            stmt.rows[1].values[0]
        }
        stmt.expectTokenRange("'Pencil'", 1, 0) {
            stmt.rows[1].values[1]
        }
        stmt.expectTokenRange("1.0", 1, 0) {
            stmt.rows[1].values[2]
        }
        stmt.expectTokenRange("?", 1, 1) {
            stmt.rows[2].values[0]
        }
        stmt.expectTokenRange("?", 2, 1) {
            stmt.rows[2].values[1]
        }
        stmt.expectTokenRange("?", 3, 1) {
            stmt.rows[2].values[2]
        }

        val conflictPolicy = stmt.conflictPolicy ?: fail()
        expect("ID, NAME") {
            conflictPolicy.columnNames.joinToString { it }
        }
        assertNull(conflictPolicy.constraintName)
        expect(1) {
            conflictPolicy.updatedActions.size
        }
        stmt.expectTokenRange("PRICE", 4, 0) {
            conflictPolicy.updatedActions[0].columnRange
        }
        stmt.expectTokenRange("EXCLUDED.PRICE", 4, 0) {
            conflictPolicy.updatedActions[0].valueRange
        }
    }

    @Test
    fun testInsertOnConflictByConstraintDoUpdate() {
        val stmt = parseSqlStatement<InsertStatement>(
                "INSERT INTO PRODUCT(ID, \"NAME\", [PRICE])" +
                        "VALUES(1, 'Pen', 3.0), (2, 'Pencil', 1.0), (?, ?, ?)" +
                        "ON CONFLICT CONSTRAINT PK_PRODUCT DO UPDATE SET " +
                        "NAME = EXCLUDED.NAME, PRICE = EXCLUDED.PRICE"
        )
        stmt.expectTokenRange("PRODUCT", 1, 0) {
            stmt.tableClauseRange
        }
        expect("ID, \"NAME\", [PRICE]") {
            stmt.insertedColumnRanges.joinToString {
                stmt.tokens[it.fromIndex].text
            }
        }
        expect(3) {
            stmt.rows.size
        }
        stmt.expectTokenRange("1", 1, 0) {
            stmt.rows[0].values[0]
        }
        stmt.expectTokenRange("'Pen'", 1, 0) {
            stmt.rows[0].values[1]
        }
        stmt.expectTokenRange("3.0", 1, 0) {
            stmt.rows[0].values[2]
        }
        stmt.expectTokenRange("2", 1, 0) {
            stmt.rows[1].values[0]
        }
        stmt.expectTokenRange("'Pencil'", 1, 0) {
            stmt.rows[1].values[1]
        }
        stmt.expectTokenRange("1.0", 1, 0) {
            stmt.rows[1].values[2]
        }
        stmt.expectTokenRange("?", 1, 1) {
            stmt.rows[2].values[0]
        }
        stmt.expectTokenRange("?", 2, 1) {
            stmt.rows[2].values[1]
        }
        stmt.expectTokenRange("?", 3, 1) {
            stmt.rows[2].values[2]
        }

        val conflictPolicy = stmt.conflictPolicy ?: fail()
        assertTrue {
            conflictPolicy.columnNames.isEmpty()
        }
        expect("PK_PRODUCT") {
            conflictPolicy.constraintName
        }
        expect(2) {
            conflictPolicy.updatedActions.size
        }
        stmt.expectTokenRange("NAME", 4, 0) {
            conflictPolicy.updatedActions[0].columnRange
        }
        stmt.expectTokenRange("EXCLUDED.NAME", 4, 0) {
            conflictPolicy.updatedActions[0].valueRange
        }
        stmt.expectTokenRange("PRICE", 4, 0) {
            conflictPolicy.updatedActions[1].columnRange
        }
        stmt.expectTokenRange("EXCLUDED.PRICE", 4, 0) {
            conflictPolicy.updatedActions[1].valueRange
        }
    }

    @Test
    fun testInsertOnDuplicateKeyUpdate() {
        val stmt = parseSqlStatement<InsertStatement>(
                "INSERT INTO PRODUCT(ID, \"NAME\", [PRICE])" +
                        "VALUES(1, 'Pen', 3.0), (2, 'Pencil', 1.0), (?, ?, ?)" +
                        "ON DUPLICATE KEY UPDATE " +
                        "NAME = EXCLUDED.NAME, PRICE = EXCLUDED.PRICE"
        )
        stmt.expectTokenRange("PRODUCT", 1, 0) {
            stmt.tableClauseRange
        }
        expect("ID, \"NAME\", [PRICE]") {
            stmt.insertedColumnRanges.joinToString {
                stmt.tokens[it.fromIndex].text
            }
        }
        expect(3) {
            stmt.rows.size
        }
        stmt.expectTokenRange("1", 1, 0) {
            stmt.rows[0].values[0]
        }
        stmt.expectTokenRange("'Pen'", 1, 0) {
            stmt.rows[0].values[1]
        }
        stmt.expectTokenRange("3.0", 1, 0) {
            stmt.rows[0].values[2]
        }
        stmt.expectTokenRange("2", 1, 0) {
            stmt.rows[1].values[0]
        }
        stmt.expectTokenRange("'Pencil'", 1, 0) {
            stmt.rows[1].values[1]
        }
        stmt.expectTokenRange("1.0", 1, 0) {
            stmt.rows[1].values[2]
        }
        stmt.expectTokenRange("?", 1, 1) {
            stmt.rows[2].values[0]
        }
        stmt.expectTokenRange("?", 2, 1) {
            stmt.rows[2].values[1]
        }
        stmt.expectTokenRange("?", 3, 1) {
            stmt.rows[2].values[2]
        }

        val conflictPolicy = stmt.conflictPolicy ?: fail()
        assertTrue {
            conflictPolicy.columnNames.isEmpty()
        }
        assertNull(conflictPolicy.constraintName)
        expect(2) {
            conflictPolicy.updatedActions.size
        }
        stmt.expectTokenRange("NAME", 4, 0) {
            conflictPolicy.updatedActions[0].columnRange
        }
        stmt.expectTokenRange("EXCLUDED.NAME", 4, 0) {
            conflictPolicy.updatedActions[0].valueRange
        }
        stmt.expectTokenRange("PRICE", 4, 0) {
            conflictPolicy.updatedActions[1].columnRange
        }
        stmt.expectTokenRange("EXCLUDED.PRICE", 4, 0) {
            conflictPolicy.updatedActions[1].valueRange
        }
    }

    @Test
    fun testInsertFrom() {
        assertFailsWith(SQLException::class, "line 1, column 22: 'FROM' is not supported") {
            parseSqlStatements("INSERT INTO PRODUCT p FROM X")
        }
    }

    @Test
    fun testInsertSelect() {
        assertFailsWith(SQLException::class, "line 1, column 22: 'SELECT' is not supported") {
            parseSqlStatements("INSERT INTO PRODUCT SELECT * FROM PRODUCT2")
        }
    }

    @Test
    fun testInsertSubSelect() {
        assertFailsWith(
                SQLException::class,
                "No inserted rows is specified by 'values', inserted by sub query is not supported"
        ) {
            parseSqlStatements("INSERT INTO PRODUCT (SELECT * FROM PRODUCT2")
        }
    }
}