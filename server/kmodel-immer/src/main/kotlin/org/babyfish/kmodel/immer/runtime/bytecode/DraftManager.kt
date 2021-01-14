package org.babyfish.kmodel.immer.runtime.bytecode

import org.babyfish.kmodel.immer.runtime.metadata.ImmerType

object DraftManager : Manager<DraftGenerator>() {

    override fun createGenerator(immerType: ImmerType): DraftGenerator =
        DraftGenerator(immerType)
}