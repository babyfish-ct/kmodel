package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import org.babyfish.kmodel.jdbc.SqlLexer
import java.util.*

internal abstract class StatementBuilder<C: ChannelType>(
    private val baseParamOffset: Int,
    private val useDepth: Boolean
) {

    private val _tokens = mutableListOf<Token>()

    private val _channels = channels()

    private var channelIndex = -1

    private var _depth = 0

    private val _paramOffsetMap = TreeMap<Int, Int>().also {
        it[0] = baseParamOffset
    }

    val tokens: List<Token>
        get() = _tokens

    val paramOffsetMap: NavigableMap<Int, Int> =
        Collections.unmodifiableNavigableMap(_paramOffsetMap)

    val channel: C?
        get() = if (channelIndex == -1) {
            null
        } else {
            _channels[channelIndex]
        }

    protected val depth: Int
        get() = _depth

    fun append(token: Token) {
        if (useDepth) {
            when (token.text) {
                "(" -> ++_depth
                ")" -> _depth--
            }
        }
        val index = _tokens.size
        if (token.text == "?") {
            _paramOffsetMap[index + 1] =
                baseParamOffset + _paramOffsetMap.size
        }
        if (token.type == SqlLexer.IDENTIFIER && depth == 0) {
            for (chIdx in _channels.indices) {
                val ch = _channels[chIdx]
                val allowed = when (ch.preDependencyType) {
                    PreDependencyType.NONE -> true
                    PreDependencyType.PREV_ALL -> channelIndex <= chIdx
                    PreDependencyType.PREV_ONE -> channelIndex + 1 == chIdx || channelIndex == chIdx
                }
                if (allowed) {
                    for (kw in ch.keywords) {
                        if (kw.equals(token.text, true)) {
                            val changed = channelIndex != chIdx
                            channelIndex = chIdx
                            keyword(kw.toLowerCase(), index, changed)
                            _tokens += token
                            return
                        }
                    }
                }
            }
        }
        _tokens += token
        accept(token, index)
    }

    fun build(): Statement? {
        if (_tokens.isEmpty()) {
            return null
        }
        return create()
    }

    protected abstract fun accept(token: Token, index: Int)

    protected abstract fun create(): Statement

    protected abstract fun channels(): Array<C>

    protected open fun keyword(text: String, index: Int, channelChanged: Boolean) {}

    internal fun tokenRange(
        fromIndex: Int,
        toIndex: Int = -1
    ) : TokenRange =
        createTokenRange(
            tokens,
            _paramOffsetMap,
            fromIndex,
            toIndex
        )

    internal fun tableAlias(fromIndex: Int, toIndex: Int): String? {
        var lastIdentifier: String? = null
        for (index in toIndex - 1 downTo fromIndex + 1) {
            if (tokens[index].type == SqlLexer.IDENTIFIER) {
                if (lastIdentifier === null) {
                    lastIdentifier = tokens[index].text
                } else {
                    return lastIdentifier
                }
            } else if (tokens[index].type != SqlLexer.WS) {
                return null
            }
        }
        return null
    }
}
