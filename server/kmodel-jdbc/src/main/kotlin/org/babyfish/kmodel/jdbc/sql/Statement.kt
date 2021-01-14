package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.*
import org.babyfish.kmodel.jdbc.SqlLexer
import java.lang.StringBuilder
import java.sql.SQLException
import java.util.*

abstract class Statement(
        val tokens: List<Token>,
        val paramOffsetMap: NavigableMap<Int, Int>
) {
    override fun toString(): String =
            """{
            |sql = ${tokens.joinToString("") { it.text }},
            |}""".trimMargin()

    val sql: String by lazy {
        tokens.joinToString("") { it.text }
    }

    fun tokenRangeText(tokenRange: TokenRange): String =
        if (tokenRange.fromIndex + 1 == tokenRange.toIndex) {
            tokens[tokenRange.fromIndex].text
        } else {
            StringBuilder().let {
                for (index in tokenRange.fromIndex until tokenRange.toIndex) {
                    it.append(tokens[index].text)
                }
                it.toString()
            }
        }

    fun createTokenRange(
        formIndex: Int,
        toIndex: Int
    ): TokenRange =
        createTokenRange(
            tokens,
            paramOffsetMap,
            formIndex,
            toIndex
        )
}

internal fun parseSqlStatements(sql: String): List<Statement> {
    val tokenStream = CommonTokenStream(
            SqlLexer(
                    CharStreams.fromString(sql)
            ).apply {
                addErrorListener(ErrorListenerImpl(sql))
            }
    )
    tokenStream.fill()
    var statementListBuilder = StatementListBuilder(sql)
    tokenStream.tokens.forEach {
        statementListBuilder.append(it)
    }
    return statementListBuilder.build()
}

private class ErrorListenerImpl(
        private val sql: String
) : BaseErrorListener() {

    override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String?,
            e: RecognitionException?
    ) {
        illegalSql(sql, msg, line, charPositionInLine)
    }
}
