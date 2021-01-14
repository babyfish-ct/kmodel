package org.babyfish.kmodel.immer.jackson

import com.fasterxml.jackson.core.json.PackageVersion
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.module.kotlin.KotlinModule

open class ImmerModule : SimpleModule(PackageVersion.VERSION) {

    override fun getDependencies(): MutableIterable<Module> = mutableListOf(KotlinModule())

    override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        context.getOwner<ObjectMapper>().setFilterProvider(
            SimpleFilterProvider().apply {
                addFilter("immerPropertyFilter", ImmerPropertyFilter())
            }
        )
    }
}

inline fun immerObjectMapper() =
    ObjectMapper().registerImmerModule()

inline fun ObjectMapper.registerImmerModule(): ObjectMapper =
    registerModule(ImmerModule())
