package org.babyfish.kmodel.immer.runtime.metadata

import org.babyfish.kmodel.immer.Immer
import org.babyfish.kmodel.immer.runtime.using
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.LinkedHashMap

class ImmerType private constructor(
    val interfaceType: Class<*>,
    superImmerTypes: List<ImmerType>
) {
    val declaredPropertyMap: Map<String, ImmerProperty> =
        interfaceType
            .declaredMethods
            .filter { !Modifier.isStatic(it.modifiers) }
            .map {
                ImmerProperty(this, it)
            }
            .associateByTo(
                LinkedHashMap()
            ) {
                it.name
            }

    val propertyMap: Map<String, ImmerProperty> by lazy {
        if (superImmerTypes.isEmpty()) {
            declaredPropertyMap
        } else {
            LinkedHashMap<String, ImmerProperty>().apply {
                superImmerTypes.forEach {
                    putAll(it.propertyMap)
                }
                putAll(declaredPropertyMap)
            }
        }
    }

    val properties: List<ImmerProperty> by lazy {
        propertyMap.values.toList()
    }

    companion object {

        private val instanceMap: MutableMap<Class<*>, ImmerType> = WeakHashMap()

        private val instanceLock: ReadWriteLock = ReentrantReadWriteLock()

        operator fun get(interfaceType: Class<*>): ImmerType =
            instanceLock.readLock().using {
                instanceMap[interfaceType]
            } ?: instanceLock.writeLock().using {
                getWithoutLock(interfaceType)
            }

        private fun getWithoutLock(interfaceType: Class<*>): ImmerType =
            instanceMap[interfaceType] ?:
                    create(interfaceType).apply {
                        instanceMap[interfaceType] = this
                    }

        private fun create(interfaceType: Class<*>): ImmerType {
            if (!interfaceType.isInterface) {
                throw IllegalClassException("")
            }
            if (!interfaceType.isAnnotationPresent(Immer::class.java)) {
                throw IllegalClassException("")
            }
            return ImmerType(
                interfaceType,
                interfaceType.interfaces.map(this::getWithoutLock)
            )
        }
    }
}