package org.babyfish.kmodel.immer.runtime.bytecode

import org.babyfish.kmodel.immer.runtime.metadata.ImmerType

object ImplManager : Manager<ImplGenerator>() {

    override fun createGenerator(immerType: ImmerType): ImplGenerator =
        ImplGenerator(immerType)
}