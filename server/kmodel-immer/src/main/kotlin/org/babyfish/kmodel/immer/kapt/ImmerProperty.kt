package org.babyfish.kmodel.immer.kapt

import com.squareup.kotlinpoet.TypeName

interface ImmerProperty {
    val name: String
    val typeName: TypeName
    val draftTypeName: TypeName?
}