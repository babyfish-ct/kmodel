package org.babyfish.kmodel.jdbc

import org.objectweb.asm.*
import java.lang.IllegalArgumentException
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Supplier
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod

internal inline fun <reified T: Any> lazyProxy(
    interceptorType: KClass<*>? = null,
    noinline creator: () -> T
): T =
    lazyProxy(T::class, interceptorType, creator)

internal fun <T: Any> lazyProxy(
    interfaceType: KClass<T>,
    interceptorType: KClass<*>? = null,
    creator: () -> T
): T =
    factory(
        LazyProxyKey(interfaceType, interceptorType)
    ).create(Supplier {
        creator()
    }) as T

private val factoryLock = ReentrantReadWriteLock()

private val factoryMap = mutableMapOf<LazyProxyKey, LazyProxyFactory>()

internal interface LazyProxyFactory {
    fun create(supplier: Supplier<Any>): Any
}

abstract class LazyProxy<T>(
    private val supplier: Supplier<T>
) {

    private val listeners = mutableListOf<(T) -> Unit>()

    private var _target: T? = null

    fun addTargetInitializedListener(
        listener: (T) -> Unit
    ) {
        listeners += listener
    }

    fun removeTargetInitializedListener(
        listener: (T) -> Unit
    ) {
        listeners -= listener
    }

    protected fun onTargetInitialized(target: T) {
        for (listener in listeners) {
            listener(target)
        }
    }

    abstract val isTargetInitialized: Boolean

    protected fun get(): T? = _target

    protected fun getOrInitialize(): T {
        return _target
            ?: supplier
                .get()
                .also {
                    _target = it
                    onTargetInitialized(it)
                }
    }

    protected val targetRef: LazyTargetRef<T> =
        object: LazyTargetRef<T> {

            override fun get(): T? =
                _target

            override fun getOrInitialize(): T =
                this@LazyProxy.getOrInitialize()
        }
}

private data class LazyProxyKey(
    val interfaceType: KClass<*>,
    val interceptorType: KClass<*>?
)

interface LazyTargetRef<T> {
    fun get(): T?
    fun getOrInitialize(): T
}

private fun factory(key: LazyProxyKey): LazyProxyFactory =
    factoryLock.read {
        factoryMap[key]
    } ?: factoryLock.write {
        factoryMap[key] ?:
        createFactory(key).also {
            factoryMap[key] = it
        }
    }

private fun createFactory(
    key: LazyProxyKey
): LazyProxyFactory {
    val (interfaceType, interceptorType) = key
    if (interfaceType.qualifiedName === null) {
        throw IllegalArgumentException(
            "Interface type \"$interfaceType\" is not valid type with qualifiedName"
        )
    }
    if (!interfaceType.java.isInterface) {
        throw IllegalArgumentException(
            "Interface type \"$interfaceType\" is not interface"
        )
    }
    val interceptorFunctionMap = mutableMapOf<KFunction<*>, KFunction<*>>()
    if (interceptorType !== null) {
        if (interceptorType.isSubclassOf(interfaceType)) {
            throw IllegalArgumentException(
                "Interceptor type \"$interceptorType\" cannot be " +
                        "sub type of interface \"$interfaceType\""
            )
        }
        if (interceptorType.isAbstract) {
            throw IllegalArgumentException(
                "Interceptor type \"$interceptorType\" " +
                        "cannot be abstract"
            )
        }
        interceptorType.constructors.firstOrNull {
            it.parameters.isEmpty()
        }?.also {
            if (it.visibility != KVisibility.PUBLIC) {
                throw IllegalArgumentException(
                    "Interceptor type \"$interceptorType\" " +
                            "does not support public constructor"
                )
            }
        } ?: throw IllegalArgumentException(
            "Interceptor type \"$interceptorType\" " +
                    "does not support default constructor"
        )
        for (interceptorFunction in interceptorType.memberFunctions) {
            if (interceptorFunction.javaMethod?.declaringClass != Object::class.java) {
                if (interceptorFunction.parameters[0].kind != KParameter.Kind.INSTANCE) {
                    throw IllegalArgumentException(
                        "Interceptor type \"$interceptorType\" is illegal, " +
                                "it's function \"$interceptorFunction\" must be instance function"
                    )
                }
                if (interceptorFunction.parameters.size < 2 ||
                        interceptorFunction.parameters[1].type.classifier != LazyTargetRef::class) {
                    throw IllegalArgumentException(
                        "Interceptor type \"$interceptorType\" is illegal, " +
                                "the first parameter of function \"$interceptorFunction\" must be " +
                                "\"${LazyTargetRef::class}\""
                    )
                }
                val targetType = interceptorFunction.parameters[1].type.arguments[0].type!!
                if (targetType.classifier == null || 
                    targetType.classifier == Any::class ||
                        targetType.classifier == Object::class) {
                    throw IllegalArgumentException(
                        "Interceptor type \"$interceptorType\" is illegal, " +
                                "the first parameter of function \"$interceptorFunction\" " +
                                "uses \"$targetType\" to be the generic argument of LazyTargetRef"
                    )
                }
                if (!interfaceType.isSubclassOf(targetType.classifier as KClass<*>)) {
                    throw IllegalArgumentException(
                        "Interceptor type \"$interceptorType\" is illegal, " +
                                "the first parameter of function \"$interceptorFunction\" " +
                                "uses \"$targetType\" to be the generic argument of LazyTargetRef but" +
                                "that type is not \"$interfaceType\" of its super type"
                    )
                }
                val function = interfaceType.memberFunctions.firstOrNull { 
                    it.name == interceptorFunction.name &&
                            it.parameters.size + 1 == interceptorFunction.parameters.size &&
                            it.parameters[0].kind == KParameter.Kind.INSTANCE &&
                            it
                                .parameters
                                .subList(1, it.parameters.size)
                                .map { p -> p.type } ==
                            interceptorFunction
                                .parameters
                                .subList(2, interceptorFunction.parameters.size)
                                .map { p -> p.type }
                }
                if (function === null) {
                    throw IllegalArgumentException(
                        "Interceptor type \"$interceptorType\" is illegal, " +
                                "its function \"$interceptorFunction\" can not match " +
                                "any function of \"$interfaceType\""
                    )
                }
                if (interceptorFunction.returnType != function.returnType) {
                    throw IllegalArgumentException(
                        "Interceptor type \"$interceptorType\" is illegal, " +
                                "its function \"$interceptorFunction\" " +
                                "returns \"${interceptorFunction.returnType}\" but " +
                                "the intercepted function \"$function\" " +
                                "returns \"${function.returnType}\""
                    )
                }
                interceptorFunctionMap[function] = interceptorFunction
            }
        }
    }
    createProxyClass(key, interceptorFunctionMap)
    return createFactoryClass(key)
        .getDeclaredConstructor()
        .newInstance() as LazyProxyFactory
}

private fun createFactoryClass(key: LazyProxyKey): Class<*> {
    val (interfaceType) = key
    val className = proxyFactoryClassName(key)
    val internalName = className
        .replace('.', '/')
    val proxyInternalName = proxyClassName(key)
        .replace('.', '/')

    val bytecode = ClassWriter(
        ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES
    ).apply {
        visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            internalName,
            null,
            "java/lang/Object",
            arrayOf(Type.getInternalName(LazyProxyFactory::class.java))
        )

        visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        ).apply {
            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
            )
            visitInsn(Opcodes.RETURN)
            visitMaxs(0, 0)
            visitEnd()
        }

        visitMethod(
            Opcodes.ACC_PUBLIC,
            "create",
            "(${Type.getDescriptor(Supplier::class.java)})Ljava/lang/Object;",
            null,
            null
        ).apply {
            visitCode()

            visitTypeInsn(
                Opcodes.NEW,
                proxyInternalName
            )
            visitInsn(Opcodes.DUP)
            visitVarInsn(Opcodes.ALOAD, 1)
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                proxyInternalName,
                "<init>",
                "(${Type.getDescriptor(Supplier::class.java)})V",
                false
            )
            visitInsn(Opcodes.ARETURN)
            visitMaxs(0, 0)
            visitEnd()
        }

        visitEnd()
    }.toByteArray()

    return DEFINE_CLASS(
        interfaceType.java.classLoader ?: DEFAULT_CLASS_LOADER,
        bytecode,
        0,
        bytecode.size
    )
}

private fun createProxyClass(
    key: LazyProxyKey,
    interceptorFunctionMap: Map<KFunction<*>, KFunction<*>>
): Class<*> {
    val (interfaceType, interceptorType) = key
    val className = proxyClassName(key)
    val internalName = className.replace('.', '/')
    val interfaceInternalName =
        interfaceType.qualifiedName!!.replace('.', '/')
    val interfaceDesc = "L$interfaceInternalName;"
    val bytecode = ClassWriter(
        ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES
    ).apply {
        visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            internalName,
            null,
            Type.getInternalName(LazyProxy::class.java),
            arrayOf(interfaceInternalName)
        )

        interceptorType?.let {
            visitField(
                Opcodes.ACC_PRIVATE,
                "interceptor",
                Type.getDescriptor(it.java),
                null,
                null
            ).visitEnd()
        }

        visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "(${Type.getDescriptor(Supplier::class.java)})V",
            null,
            null
        ).apply {
            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitVarInsn(Opcodes.ALOAD, 1)
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                Type.getInternalName(LazyProxy::class.java),
                "<init>",
                "(${Type.getDescriptor(Supplier::class.java)})V",
                false
            )
            interceptorType?.let {
                visitVarInsn(Opcodes.ALOAD, 0)
                visitTypeInsn(
                    Opcodes.NEW,
                    Type.getInternalName(it.java)
                )
                visitInsn(Opcodes.DUP)
                visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    Type.getInternalName(it.java),
                    "<init>",
                    "()V",
                    false
                )
                visitFieldInsn(
                    Opcodes.PUTFIELD,
                    internalName,
                    "interceptor",
                    Type.getDescriptor(it.java)
                )
            }
            visitInsn(Opcodes.RETURN)
            visitMaxs(0, 0)
            visitEnd()
        }

        visitMethod(
            Opcodes.ACC_PUBLIC,
            "isTargetInitialized",
            "()Z",
            null,
            null
        ).apply {

            val elseLabel = Label()

            visitCode()

            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                internalName,
                "get",
                "()Ljava/lang/Object;",
                false
            )
            visitJumpInsn(Opcodes.IFNULL, elseLabel)
            visitInsn(Opcodes.ICONST_1)
            visitInsn(Opcodes.IRETURN)
            visitLabel(elseLabel)
            visitInsn(Opcodes.ICONST_0)
            visitInsn(Opcodes.IRETURN)

            visitMaxs(0, 0)
            visitEnd()
        }

        interfaceType.memberFunctions.forEach {
            val desc = Type.getMethodDescriptor(it.javaMethod)
            visitMethod(
                Opcodes.ACC_PUBLIC,
                it.name,
                desc,
                null,
                null
            ).visitFunction(
                it,
                interceptorFunctionMap[it],
                interfaceType,
                interceptorType,
                desc,
                internalName,
                interfaceInternalName,
                interfaceDesc
            )
        }

        visitEnd()
    }.toByteArray()

    return DEFINE_CLASS(
        interfaceType.java.classLoader ?: DEFAULT_CLASS_LOADER,
        bytecode,
        0,
        bytecode.size
    )
}

private fun MethodVisitor.visitFunction(
    function: KFunction<*>,
    interceptorFunction: KFunction<*>?,
    interfaceType: KClass<*>,
    interceptorType: KClass<*>?,
    methodDesc: String,
    proxyInternalName: String,
    interfaceInternalName: String,
    interfaceDesc: String
): KFunction<*>? {

    visitCode()

    if (interceptorFunction !== null) {
        visitVarInsn(Opcodes.ALOAD, 0)
        visitFieldInsn(
            Opcodes.GETFIELD,
            proxyInternalName,
            "interceptor",
            Type.getDescriptor(interceptorType!!.java)
        )
        visitVarInsn(Opcodes.ALOAD, 0)
        visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            proxyInternalName,
            "getTargetRef",
            "()${Type.getDescriptor(LazyTargetRef::class.java)}",
            false
        )
    } else {
        visitVarInsn(Opcodes.ALOAD, 0)
        visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            proxyInternalName,
            "getOrInitialize",
            "()Ljava/lang/Object;",
            false
        )
        visitTypeInsn(
            Opcodes.CHECKCAST,
            interfaceInternalName
        )
    }

    var slot = 1
    for (parameter in function.parameters) {
        if (parameter.kind != KParameter.Kind.INSTANCE) {
            visitVarInsn(loadCode(parameter.type), slot)
            slot += slotSize(parameter.type)
        }
    }
    if (interceptorFunction !== null) {
        val targetParamDesc = Type.getDescriptor(
            (interceptorFunction.parameters[1].type.classifier as KClass<*>).java
        )
        visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            Type.getInternalName(interceptorType!!.java),
            interceptorFunction.name,
            "(${targetParamDesc}${methodDesc.substring(1)}",
            false
        )
    } else {
        visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            interfaceInternalName,
            function.name,
            methodDesc,
            true
        )
    }
    visitInsn(returnCode(function.returnType))

    visitMaxs(0, 0)
    visitEnd()

    return interceptorFunction
}

private fun loadCode(type: KType): Int =
    when (type.classifier) {
        Boolean::class -> Opcodes.ILOAD
        Char::class -> Opcodes.ILOAD
        Byte::class -> Opcodes.ILOAD
        UByte::class -> Opcodes.ILOAD
        Short::class -> Opcodes.ILOAD
        UShort::class -> Opcodes.ILOAD
        Int::class -> Opcodes.ILOAD
        UInt::class -> Opcodes.ILOAD
        Long::class -> Opcodes.LLOAD
        ULong::class -> Opcodes.LLOAD
        Float::class -> Opcodes.FLOAD
        Double::class -> Opcodes.DLOAD
        else -> Opcodes.ALOAD
    }

private fun returnCode(type: KType): Int =
    when (type.classifier) {
        Boolean::class -> Opcodes.IRETURN
        Char::class -> Opcodes.IRETURN
        Byte::class -> Opcodes.IRETURN
        UByte::class -> Opcodes.IRETURN
        Short::class -> Opcodes.IRETURN
        UShort::class -> Opcodes.IRETURN
        Int::class -> Opcodes.IRETURN
        UInt::class -> Opcodes.IRETURN
        Long::class -> Opcodes.LRETURN
        ULong::class -> Opcodes.LRETURN
        Float::class -> Opcodes.FRETURN
        Double::class -> Opcodes.DRETURN
        Unit::class -> Opcodes.RETURN
        else -> Opcodes.ARETURN
    }

private fun slotSize(type: KType): Int =
    when (type.classifier) {
        Long::class -> 2
        Double::class -> 2
        else -> 1
    }

private val DEFINE_CLASS =
    ClassLoader::class
        .memberFunctions
        .first {
            it.name == "defineClass" &&
                    it.parameters.size == 4 &&
                    it.parameters[0].type.classifier == ClassLoader::class
                    it.parameters[1].type.classifier == ByteArray::class &&
                    it.parameters[2].type.classifier == Int::class &&
                    it.parameters[3].type.classifier == Int::class
        }.apply {
            this.javaMethod?.isAccessible = true
        }  as KFunction4<ClassLoader, ByteArray, Int, Int, Class<*>>

private fun proxyClassName(key: LazyProxyKey) =
    key.interfaceType.qualifiedName!!
        .let {
            if (it.startsWith("java.") || it.startsWith("javax.")) {
                "${LazyProxyFactory::class.java.`package`.name}.$it${interceptorSuffix(
                    key,
                    false
                )}"
            } else {
                "$it${interceptorSuffix(key, false)}"
            }
        }

private fun proxyFactoryClassName(key: LazyProxyKey) =
    key.interfaceType.qualifiedName!!
        .let {
            if (it.startsWith("java.") || it.startsWith("javax.")) {
                "${LazyProxyFactory::class.java.`package`.name}.$it${interceptorSuffix(
                    key,
                    true
                )}"
            } else {
                "$it${interceptorSuffix(key, true)}"
            }
        }

private fun interceptorSuffix(key: LazyProxyKey, factory: Boolean): String {
    val text = if (factory) {
        "LazyProxyFactory"
    } else {
        "LazyProxy"
    }
    return key.interceptorType?.let {
        "{$text-${it.simpleName}}"
    } ?: "{$text}"
}

private val DEFAULT_CLASS_LOADER =
    LazyProxyFactory::class.java.classLoader