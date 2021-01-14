package org.babyfish.kmodel.immer.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import org.babyfish.kmodel.immer.Draft
import org.babyfish.kmodel.immer.DraftContext
import java.lang.IllegalArgumentException
import java.lang.StringBuilder

internal val KOTLIN_PRIMITIVE_NAMES = setOf(
    "Boolean",
    "Char",
    "Byte",
    "Short",
    "Int",
    "Long",
    "Float",
    "Double"
)

internal val JAVA_PRIMITIVE_NAMES = setOf(
    "Boolean",
    "Character",
    "Byte",
    "Short",
    "Integer",
    "Long",
    "Float",
    "Double"
)

internal const val DRAFT_POSTFIX = "Draft"

internal val DRAFT_CLASS_NAME = ClassName(
    Draft::class.java.`package`.name,
    Draft::class.simpleName!!
)

internal val DRAFT_CONTEXT_CLASS_NAME = ClassName(
    DraftContext::class.java.`package`.name,
    DraftContext::class.simpleName!!
)

internal const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

internal fun camelCase(name: String): String {
    val builder = StringBuilder()
    for (i in 0 until name.length) {
        val c = name[i]
        if (c.isUpperCase()) {
            builder.append(c.toLowerCase())
        } else {
            builder.append(name.substring(i))
            break
        }
    }
    return builder.toString()
}

internal fun ClassName.toDraft(): ClassName =
    ClassName(
        this.packageName(),
        "${this.simpleName()}$DRAFT_POSTFIX"
    )

internal fun TypeName.toClassName(): ClassName =
    if (this is ClassName) {
        this
    } else if (this is ParameterizedTypeName) {
        rawType
    } else {
        throw IllegalArgumentException()
    }