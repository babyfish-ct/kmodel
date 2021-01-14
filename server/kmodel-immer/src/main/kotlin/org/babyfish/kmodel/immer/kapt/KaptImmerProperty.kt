package org.babyfish.kmodel.immer.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import org.babyfish.kmodel.immer.Immer
import org.jetbrains.annotations.NotNull
import javax.lang.model.element.ExecutableElement

class KaptImmerProperty(
    declaringImmerType: ImmerType,
    element: ExecutableElement
) : AbstractImmerProperty(
    declaringImmerType
) {

    override val name: String

    override val typeName: TypeName
    
    init {
        val methodName = element.simpleName.toString()
        name = when {
            methodName.startsWith("is") &&
                    element.returnType.asTypeName() ==
                    ClassName("kotlin", "Boolean") ->
                camelCase(methodName.substring(2))
            methodName.startsWith("get") ->
                camelCase(methodName.substring(3))
            else ->
                throw IllegalSourceCodeException(
                    "The interface ${declaringImmerType.interfaceName} " +
                            "uses the annotation @${Immer::class.qualifiedName}, " +
                            "but its method '$methodName' is not a getter method"
                )
        }
        if (element.parameters.isNotEmpty()) {
            throw IllegalSourceCodeException(
                "The interface ${declaringImmerType.interfaceName} " +
                        "uses the annotation @${Immer::class.qualifiedName}, " +
                        "so this method must have no parameters"
            )
        }

        val aptTypeName = element.returnType.asTypeName()
        val isKotlinPrimitiveType =
            aptTypeName is ClassName &&
                    aptTypeName.packageName() == "kotlin" &&
                    KOTLIN_PRIMITIVE_NAMES.contains(aptTypeName.simpleName())
        val isJavaPrimitiveType =
            aptTypeName is ClassName &&
                    aptTypeName.packageName() == "java.lang" &&
                    JAVA_PRIMITIVE_NAMES.contains(aptTypeName.simpleName())
        val nonNullTypeName: TypeName =
            when {
                aptTypeName is ClassName -> aptTypeName
                aptTypeName is ParameterizedTypeName -> aptTypeName.rawType
                else -> throw IllegalSourceCodeException(
                    "The interface ${declaringImmerType.interfaceName} " +
                            "uses the annotation @${Immer::class.qualifiedName}, " +
                            "the return type $aptTypeName is not allowed"
                )
            }.let {
                when {
                    isJavaPrimitiveType ->
                        ClassName(
                            "kotlin",
                            when (it.simpleName()) {
                                "Character" -> "Char"
                                "Integer" -> "Int"
                                else -> it.simpleName()
                            }
                        )
                    it.packageName() == "java.lang" && it.simpleName() == "String" ->
                        ClassName("kotlin", "String")
                    it.packageName() == "java.util" && it.simpleName() == "List" -> {
                        if (aptTypeName !is ParameterizedTypeName) {
                            throw IllegalSourceCodeException(
                                "The interface ${declaringImmerType.interfaceName} " +
                                        "uses the annotation @${Immer::class.qualifiedName} " +
                                        "the return type $aptTypeName is collection but does not use generic"
                            )
                        }
                        val elementTypeName = aptTypeName.typeArguments[0]
                        if (elementTypeName !is ClassName) {
                            throw IllegalSourceCodeException(
                                "The interface ${declaringImmerType.interfaceName} " +
                                        "uses the annotation @${Immer::class.qualifiedName}, " +
                                        "the return type $aptTypeName use the $elementTypeName " +
                                        "to be its element type but that element type is not class"
                            )
                        }
                        ParameterizedTypeName.get(
                            ClassName("kotlin.collections", "List"),
                            ClassName(
                                declaringImmerType.interfaceName.packageName(),
                                "${elementTypeName.simpleName()}"
                            )
                        )
                    }
                    it.packageName() == "kotlin" && it.simpleName() == "Array" -> {
                        if (aptTypeName !is ParameterizedTypeName) {
                            throw IllegalSourceCodeException(
                                "The interface ${declaringImmerType.interfaceName} " +
                                        "uses the annotation @${Immer::class.qualifiedName}, " +
                                        "the return type $aptTypeName is array but does not use generic"
                            )
                        }
                        aptTypeName
                            .typeArguments[0]
                            .let { tn ->
                                if (tn !is ClassName) {
                                    throw IllegalSourceCodeException(
                                        "The interface ${declaringImmerType.interfaceName} " +
                                                "uses the annotation @${Immer::class.qualifiedName}, " +
                                                "the return type $aptTypeName is array " +
                                                "but its generic argument $tn is not class"
                                    )
                                }
                                if (tn.packageName() != "kotlin" ||
                                    !KOTLIN_PRIMITIVE_NAMES.contains(tn.simpleName())
                                ) {
                                    throw IllegalSourceCodeException(
                                        "The interface ${declaringImmerType.interfaceName} " +
                                                "uses the annotation @${Immer::class.qualifiedName}, " +
                                                "the return type $aptTypeName is array " +
                                                "but its generic argument $tn is not primitive type"
                                    )
                                }
                                ClassName("kotlin", "${tn.simpleName()}Array")
                            }
                    }
                    else -> it
                }
            }
        
        typeName = nonNullTypeName
            .let {
                if (isJavaPrimitiveType) {
                    it.asNullable()
                } else if (!isKotlinPrimitiveType && element.getAnnotation(NotNull::class.java) === null) {
                    it.asNullable()
                } else {
                    it
                }
            }
    }
}