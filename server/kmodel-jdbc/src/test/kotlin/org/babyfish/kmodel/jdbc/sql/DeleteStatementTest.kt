package org.babyfish.kmodel.jdbc.sql

import org.junit.Test

class DeleteStatementTest {

    @Test
    fun testDeleteCascade() {
        val deleteStatement = parseSqlStatement<DeleteStatement>(
            """
                delete from department
                cascade delete employee(tenant_id, department_id)
                cascade delete employee(tenant_id, supervisor_id)
            """.trimIndent()
        )
        println(deleteStatement)
    }
}