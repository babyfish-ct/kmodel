package org.babyfish.kmodel.jdbc.exec

import org.babyfish.kmodel.jdbc.metadata.Table

data class DMLMutationResult(
     val table: Table,
     val updateCount: Int,
     val beforeRowMap: Map<List<Any>, Row?>
)