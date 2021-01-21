package org.babyfish.kmodel.jdbc

import org.babyfish.kmodel.jdbc.metadata.ForeignKey

data class Configuration(
    val dataChangedListener: (DataChangedEvent) -> Unit,
    val deletionOptionSupplier: ((ForeignKey) -> DeletionOption)?
)