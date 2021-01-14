package org.babyfish.kmodel.immer.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

abstract class AbstractImmerProperty(
    val declaringImmerType: ImmerType
) : ImmerProperty {

    override val draftTypeName: TypeName? by lazy {
        typeName
            .let {
                if (it is ParameterizedTypeName &&
                    it.rawType.packageName() == "kotlin.collections" &&
                    it.rawType.simpleName() === "List") {
                    val elementTypeName = it.typeArguments[0] as ClassName
                    ParameterizedTypeName.get(
                        ClassName("kotlin.collections", "MutableList"),
                        ClassName(
                            elementTypeName.packageName(),
                            "${elementTypeName.simpleName()}${DRAFT_POSTFIX}"
                        )
                    )
                } else if (it is ClassName) {
                    when {
                        declaringImmerType.kaptContext[it] !== null ->
                            ClassName(it.packageName(), "${it.simpleName()}$DRAFT_POSTFIX")
                        else -> null
                    }
                } else {
                    error("Internal bug")
                }
            }
            ?.let {
                if (typeName.nullable) {
                    it.asNullable()
                } else {
                    it
                }
            }
    }
}