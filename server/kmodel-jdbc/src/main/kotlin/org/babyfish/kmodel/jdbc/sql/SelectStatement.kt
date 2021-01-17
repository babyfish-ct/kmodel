package org.babyfish.kmodel.jdbc.sql

import org.antlr.v4.runtime.Token
import java.util.*

class SelectStatement(
    tokens: List<Token>,
    paramOffsetMap: NavigableMap<Int, Int>
): Statement(tokens, paramOffsetMap)