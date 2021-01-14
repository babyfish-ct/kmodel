package org.babyfish.kmodel.jdbc

import org.babyfish.kmodel.jdbc.exec.Row
import org.babyfish.kmodel.jdbc.metadata.QualifiedName

data class DataChangedEvent(
    val beforeImageMap: Map<QualifiedName, Map<List<Any>, Row?>>,
    val afterImageMap: Map<QualifiedName, Map<List<Any>, Row>>
)