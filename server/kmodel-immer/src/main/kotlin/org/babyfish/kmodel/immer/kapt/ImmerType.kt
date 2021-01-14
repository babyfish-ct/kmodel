package org.babyfish.kmodel.immer.kapt

import com.squareup.kotlinpoet.*
import org.babyfish.kmodel.immer.DraftBehavior
import org.babyfish.kmodel.immer.Immer
import org.babyfish.kmodel.immer.runtime.ImmerRuntime
import java.lang.IllegalArgumentException
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import java.lang.reflect.Modifier as JModifier
import javax.lang.model.element.TypeElement

class ImmerType private constructor(
    val kaptContext: KaptContext,
    val packageName: String?,
    val typeElement: TypeElement?,
    val javaClass: Class<*>?
) {
    constructor(kaptContext: KaptContext, packageName: String, typeElement: TypeElement):
            this(kaptContext, packageName, typeElement, null)

    constructor(kaptContext: KaptContext, javaClass: Class<*>):
            this(kaptContext, null, null, javaClass)

    val interfaceName: ClassName =
        if (typeElement !== null) {
            ClassName(
                packageName!!,
                typeElement.simpleName.toString()
            )
        } else {
            ClassName(
                javaClass!!.`package`?.name ?: "",
                javaClass!!.simpleName
            )
        }

    init {
        val isInterface =
            typeElement
                ?.let { it.kind == ElementKind.INTERFACE }
                ?: javaClass!!.isInterface
        if (!isInterface) {
            throw IllegalArgumentException(
                "The type $interfaceName must be interface"
            )
        }
        val isImmer =
            typeElement
                ?.getAnnotation(Immer::class.java)
                .let { true }
                ?: javaClass!!.isAnnotationPresent(Immer::class.java)
        if (!isImmer) {
            throw IllegalArgumentException(
                "The type $interfaceName must use annotation@${Immer::class.qualifiedName}"
            )
        }
        if (interfaceName.simpleName().endsWith(DRAFT_POSTFIX)) {
            throw IllegalArgumentException(
                "The name of the interface $interfaceName" +
                        "that uses the annotation @${Immer::class.qualifiedName} " +
                        "cannot end with $DRAFT_POSTFIX"
            )
        }
    }

    val superImmerTypes: List<ImmerType> by lazy {
        if (typeElement !== null) {
            typeElement.interfaces.map {
                kaptContext[it.asTypeName().toClassName()] ?: error("")
            }
        } else {
            javaClass!!.interfaces.map {
                kaptContext[ClassName(it.`package`?.name ?: "", it.simpleName)]
                    ?: error("")
            }
        }
    }

    val declaredProperties: Map<String, ImmerProperty> by lazy {
        LinkedHashMap<String, ImmerProperty>().apply {
            if (typeElement !== null) {
                for (enclosedElement in typeElement.enclosedElements) {
                    if (enclosedElement.kind == ElementKind.METHOD &&
                        !enclosedElement.modifiers.contains(Modifier.STATIC)) {
                        KaptImmerProperty(
                            this@ImmerType,
                            enclosedElement as ExecutableElement
                        ).let {
                            this[it.name] = it
                        }
                    }
                }
            } else {
                for (method in javaClass!!.declaredMethods) {
                    if (!JModifier.isStatic(method.modifiers)) {
                        BytecodeImmerProperty(
                            this@ImmerType,
                            method
                        ).let {
                            this[it.name] = it
                        }
                    }
                }
            }
        }
    }

    val properties: Map<String, ImmerProperty> by lazy {
        if (superImmerTypes.isEmpty()) {
            declaredProperties
        } else {
            LinkedHashMap<String, ImmerProperty>().apply {
                for (superImmerType in superImmerTypes) {
                    for (superImmerProperty in superImmerType.properties.values) {
                        if (!declaredProperties.containsKey(superImmerProperty.name)) {
                            val conflictSuperImmerProperty = put(
                                superImmerProperty.name,
                                superImmerProperty
                            )
                            if (conflictSuperImmerProperty !== null &&
                                conflictSuperImmerProperty.typeName !== superImmerProperty.typeName
                            ) {
                                throw IllegalSourceCodeException("")
                            }
                        }
                    }
                }
                putAll(declaredProperties)
            }
        }
    }

    fun generate(): FileSpec =
        FileSpec
            .builder(
                interfaceName.packageName(),
                "${interfaceName.simpleName()}$DRAFT_POSTFIX"
            )
            .addAliasedImport(ImmerRuntime::class, "ImmerRuntime")
            .addType(
                TypeSpec
                    .interfaceBuilder("${interfaceName.simpleName()}$DRAFT_POSTFIX")
                    .addSuperinterface(interfaceName)
                    .addSuperinterface(DRAFT_CLASS_NAME)
                    .apply {
                        superImmerTypes.forEach {
                            addSuperinterface(
                                ClassName(
                                    it.packageName ?: "",
                                    "${it.interfaceName}$DRAFT_POSTFIX"
                                )
                            )
                        }
                    }
                    .apply {
                        for (property in declaredProperties.values) {
                            addProperty(property(property))
                            draftPropertyFun(property)?.let {
                                addFunction(it)
                            }
                        }
                    }
                    .build()
            )
            .addFunction(globalNewFunWithLambda())
            .addFunction(globalNewFunWithProperties())
            .addFunction(draftBehaviorNewFunWithLambda())
            .addFunction(draftBehaviorNewFunWithProperties())
            .addFunction(produceFun())
            .addFunction(listProduceFun())
            .build()

    private fun property(immerProperty: ImmerProperty): PropertySpec =
        PropertySpec
            .varBuilder(
                immerProperty.name,
                immerProperty.typeName,
                KModifier.OVERRIDE
            )
            .build()

    private fun draftPropertyFun(immerProperty: ImmerProperty): FunSpec? =
        immerProperty
            .draftTypeName
            ?.let {
                FunSpec
                    .builder(immerProperty.name)
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter(
                        ParameterSpec
                            .builder("autoCreate", Boolean::class)
                            .defaultValue("false")
                            .build()
                    )
                    .returns(it)
                    .build()
            }

    private fun globalNewFunWithLambda(): FunSpec =
        FunSpec
            .builder("new${interfaceName.simpleName()}")
            .addParameter(
                "draftHandler",
                LambdaTypeName.get(
                    receiver = ParameterizedTypeName.get(
                        DRAFT_CONTEXT_CLASS_NAME,
                        interfaceName.toDraft()
                    ),
                    returnType = UNIT
                )
            )
            .returns(interfaceName)
            .addCode(
                """return ImmerRuntime.modifyReference(
                    |    ${interfaceName.simpleName()}$DRAFT_POSTFIX::class,
                    |    ImmerRuntime.get(${interfaceName.simpleName()}::class)
                    |) {
                    |    DraftContext(it).draftHandler()
                    |}""".trimMargin()
            )
            .build()

    private fun globalNewFunWithProperties(): FunSpec =
        FunSpec
            .builder("new${interfaceName.simpleName()}")
            .apply {
                properties.values.forEach {
                    addParameter(
                        ParameterSpec
                            .builder(it.name, it.typeName)
                            .apply {
                                if (it.typeName.nullable) {
                                    defaultValue("null")
                                }
                            }
                            .build()
                    )
                }
            }
            .returns(interfaceName)
            .addCode(
                """return ImmerRuntime.modifyReference(
                    |    ${interfaceName.simpleName()}$DRAFT_POSTFIX::class,
                    |    ImmerRuntime.get(${interfaceName.simpleName()}::class)
                    |) {
                    |""".trimMargin()
            )
            .apply {
                properties.values.forEach {
                    addCode("    it.${it.name} = ${it.name}\n")
                }
            }
            .addCode("}")
            .build()

    private fun draftBehaviorNewFunWithLambda(): FunSpec =
        FunSpec
            .builder("new${interfaceName.simpleName()}")
            .receiver(DraftBehavior::class)
            .addParameter(
                "draftHandler",
                LambdaTypeName.get(
                    receiver = ParameterizedTypeName.get(
                        DRAFT_CONTEXT_CLASS_NAME,
                        interfaceName.toDraft()
                    ),
                    returnType = UNIT
                )
            )
            .returns(
                interfaceName.toDraft()
            )
            .addCode(
                """return ImmerRuntime.newDraft(
                    |    ${interfaceName.simpleName()}Draft::class
                    |).apply {
                    |    DraftContext(this).draftHandler()
                    |}""".trimMargin()
            )
            .build()

    private fun draftBehaviorNewFunWithProperties(): FunSpec =
        FunSpec
            .builder("new${interfaceName.simpleName()}")
            .receiver(DraftBehavior::class)
            .apply {
                properties.values.forEach {
                    addParameter(
                        ParameterSpec
                            .builder(it.name, it.typeName)
                            .apply {
                                if (it.typeName.nullable) {
                                    defaultValue("null")
                                }
                            }
                            .build()
                    )
                }
            }
            .returns(
                interfaceName.toDraft()
            )
            .addCode(
                """return ImmerRuntime.newDraft(
                    |    ${interfaceName.simpleName()}Draft::class
                    |).apply {
                    |""".trimMargin()
            )
            .apply {
                properties.values.forEach {
                    addCode("    this.${it.name} = ${it.name}\n")
                }
            }
            .addCode("}")
            .build()

    private fun produceFun(): FunSpec =
        FunSpec
            .builder("produce")
            .addParameter(
                "baseState",
                interfaceName
            )
            .addParameter(
                "draftHandler",
                LambdaTypeName.get(
                    receiver = ParameterizedTypeName.get(
                        DRAFT_CONTEXT_CLASS_NAME,
                        interfaceName.toDraft()
                    ),
                    returnType = UNIT
                )
            )
            .returns(interfaceName)
            .addCode(
                """return ImmerRuntime.modifyReference(
                    |    ${interfaceName.simpleName()}$DRAFT_POSTFIX::class,
                    |    baseState
                    |) {
                    |    DraftContext(it).draftHandler()
                    |}""".trimMargin()
            )
            .build()

    private fun listProduceFun(): FunSpec =
        FunSpec
            .builder("produce")
            .addParameter(
                "baseList",
                ParameterizedTypeName.get(
                    ClassName("kotlin.collections", "List"),
                    interfaceName
                )
            )
            .addParameter(
                "draftHandler",
                LambdaTypeName.get(
                    receiver = ParameterizedTypeName.get(
                        DRAFT_CONTEXT_CLASS_NAME,
                        ParameterizedTypeName.get(
                            ClassName(
                                "kotlin.collections",
                                "MutableList"
                            ),
                            interfaceName.toDraft()
                        )
                    ),
                    returnType = UNIT
                )
            )
            .returns(
                ParameterizedTypeName.get(
                    ClassName("kotlin.collections", "List"),
                    interfaceName
                )
            )
            .addCode(
                """return ImmerRuntime.modifyList(
                    |    ${interfaceName.simpleName()}$DRAFT_POSTFIX::class,
                    |    baseList
                    |) {
                    |    DraftContext(it).draftHandler()
                    |}""".trimMargin()
            )
            .build()
}