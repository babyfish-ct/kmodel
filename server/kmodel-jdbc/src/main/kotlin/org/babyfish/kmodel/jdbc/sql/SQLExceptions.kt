package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import java.sql.SQLException

fun illegalSql(sql: String, message: String?, line: Int, charPositionInLine: Int): Nothing {
    val nonNullMessage = message ?: "Bad lexer matcher"
    throw SQLException("Illegal SQL: [$sql], line: $line, column: $charPositionInLine, error: \"$nonNullMessage\"")
}

fun illegalSql(sql: String, message: String, token: Token? = null): Nothing {
    if (token === null) {
        throw SQLException("Illegal SQL: [$sql], error: $message")
    }
    illegalSql(sql, message, token.line, token.charPositionInLine)
}