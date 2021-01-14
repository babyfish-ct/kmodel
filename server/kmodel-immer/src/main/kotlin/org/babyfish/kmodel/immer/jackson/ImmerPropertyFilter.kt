package org.babyfish.kmodel.immer.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.PropertyWriter
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import org.babyfish.kmodel.immer.runtime.PropertyState
import org.babyfish.kmodel.immer.runtime.internal.ImmerObject

class ImmerPropertyFilter : SimpleBeanPropertyFilter() {
    override fun serializeAsField(
        pojo: Any,
        jgen: JsonGenerator,
        provider: SerializerProvider,
        writer: PropertyWriter
    ) {
        if (pojo !is ImmerObject || pojo.propertyState(writer.name) == PropertyState.AVAILABLE) {
            super.serializeAsField(pojo, jgen, provider, writer)
        }
    }
}