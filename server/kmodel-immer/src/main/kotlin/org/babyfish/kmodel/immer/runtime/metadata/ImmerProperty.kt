package org.babyfish.kmodel.immer.runtime.metadata

import org.babyfish.kmodel.immer.Immer
import org.jetbrains.annotations.NotNull
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

class ImmerProperty internal constructor(
    val declaredImmerType: ImmerType,
    val getter: Method
) {
    val name: String =
        getter
            .name
            .let {
                when {
                    it.startsWith("is") && getter.returnType === Boolean::class.java ->
                        camelCase(it.substring(2))
                    it.startsWith("get") ->
                        camelCase(it.substring(3))
                    else ->
                        throw IllegalClassException("")
                }
            }
            .also {
                if (it.isEmpty()) {
                    throw IllegalClassException("")
                }
            }

    val nullable: Boolean =
        when {
            getter.returnType.isPrimitive -> false
            BOX_TYPES.contains(getter.returnType) -> true
            else -> getter.isAnnotationPresent(NotNull::class.java) === null
        }

    val isList: Boolean =
        getter.returnType === List::class.java

    val isPrimitive: Boolean =
        getter.returnType.isPrimitive

    val isPrimitiveBox: Boolean =
        BOX_TYPES.contains(getter.returnType)

    val returnType: Class<*>
        get() = getter.returnType

    val targetType: Class<*> =
        if (isList) {
            getter
                .genericReturnType
                .let {
                    if (it !is ParameterizedType) {
                        throw IllegalClassException("")
                    }
                    it.actualTypeArguments[0]
                }
                .let {
                    if (it !is Class<*>) {
                        throw IllegalClassException("")
                    }
                    it as Class<*>
                }
        } else {
            getter.returnType
        }

    val targetImmerType: ImmerType? by lazy {
        targetType
            .takeIf { it.isAnnotationPresent(Immer::class.java) }
            ?.let { ImmerType[it] }
    }

    init {

        if (Modifier.isStatic(getter.modifiers)) {
            throw IllegalArgumentException()
        }
        if (getter.parameterTypes.isNotEmpty()) {
            throw IllegalClassException("")
        }
        if (getter.returnType === Void::class.java) {
            throw IllegalClassException("")
        }

        if (Collection::class.java.isAssignableFrom(getter.returnType)) {
            if (getter.returnType !== List::class.java) {
                throw IllegalClassException("")
            }
        } else if (Map::class.java.isAssignableFrom(getter.returnType)) {
            throw IllegalClassException("")
        }
    }

    companion object {
        private val BOX_TYPES = setOf(
            Boolean::class.java,
            Character::class.java,
            Byte::class.java,
            Short::class.java,
            Integer::class.java,
            Long::class.java,
            Float::class.java,
            Double::class.java
        )
        private fun camelCase(name: String): String {
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
    }
}