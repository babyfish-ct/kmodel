package org.babyfish.kmodel.immer.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import java.lang.reflect.Method

class BytecodeImmerProperty(
    declaringImmerType: ImmerType,
    method: Method
) : AbstractImmerProperty(declaringImmerType) {
    override val name: String

    override val typeName: TypeName

    init {
        if (method.parameterTypes.isNotEmpty()) {
            throw IllegalSourceCodeException("")
        }
        name =
            if (method.name.startsWith("is") && method.returnType == Boolean::class.java) {
                camelCase(method.name.substring(2))
            } else if (method.name.startsWith("get")) {
                camelCase(method.name.substring(3))
            } else {
                throw IllegalSourceCodeException("")
            }
        val returnType = method.returnType
        typeName = when {
            returnType.isPrimitive ->
                ClassName(
                    "koltin",
                    returnType.name.let {
                        "${it.substring(0, 1).toUpperCase()}${it.substring(1)}}"
                    }
                )
            returnType.`package`?.name == "java.lang" &&
                    JAVA_PRIMITIVE_NAMES.contains(returnType.simpleName) ->
                ClassName(
                    "kotlin",
                    "String"
                )
            else ->
                ClassName(
                    "kotlin",
                    "String"
                )
        }
    }
}