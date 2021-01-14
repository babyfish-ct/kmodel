package org.babyfish.kmodel.jdbc.sql

import org.babyfish.kmodel.test.ObjectContext
import java.lang.StringBuilder
import kotlin.test.expect
import kotlin.test.fail

internal inline fun <reified T: Statement> parseSqlStatement(sql: String): T {
    val statements = parseSqlStatements(sql)
    expect(1, "Expected single SQL") {
        statements.size
    }
    return statements[0] as? T
            ?: fail("The type of statement is not ${T::class.qualifiedName}")
}

internal fun Statement.expectTokenRange(
        sqlPart: String,
        paramOffset: Int,
        paramCount: Int,
        tokenRangeSupplier: () -> TokenRange
) {
    val tokenRange = tokenRangeSupplier()
    expect(sqlPart) {
        StringBuilder().let {
            for (index in tokenRange.fromIndex until tokenRange.toIndex) {
                it.append(tokens[index].text)
            }
            it.toString()
        }
    }
    expect(paramOffset) {
        tokenRange.paramOffset
    }
    expect(paramCount) {
        tokenRange.paramCount
    }
}

fun ObjectContext<TokenRange>.rangeEq(
    sqlPart: String,
    paramOffset: Int,
    paramCount: Int,
    statement: Statement
) {
    value("sqlPart") {
        StringBuilder().let {
            for (index in value.fromIndex until value.toIndex) {
                it.append(statement.tokens[index].text)
            }
            it.toString()
        }
    } eq sqlPart
    value(TokenRange::paramOffset) eq paramOffset
    value(TokenRange::paramCount) eq paramCount
}