package org.babyfish.kmodel.jdbc.sql

import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.expect
import kotlin.test.fail

class UpdateStatementTest {

    @Test
    fun testWithoutCondition() {

    }

    @Test
    fun testWithCondition() {
        val stmt = parseSqlStatement<UpdateStatement>(
                "UPDATE PRODUCT SET PRICE = PRICE + ? WHERE NAME LIKE ?"
        )
        stmt.expectTokenRange(
                "PRODUCT",
                1,
                0
        ) {
            stmt.tableSourceRange
        }
        expect(1) {
            stmt.updatedActions.size
        }
        stmt.expectTokenRange(
                "PRICE",
                1,
                0
        ) {
            stmt.updatedActions[0].columnRange
        }
        stmt.expectTokenRange(
                "PRICE + ?",
                1,
                1
        ) {
            stmt.updatedActions[0].valueRange
        }
        stmt.expectTokenRange(
                "NAME LIKE ?",
                2,
                1
        ) {
            stmt.conditionRange ?: fail()
        }
    }
}