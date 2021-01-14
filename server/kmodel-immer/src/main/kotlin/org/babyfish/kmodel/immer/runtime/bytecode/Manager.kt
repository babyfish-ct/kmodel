package org.babyfish.kmodel.immer.runtime.bytecode

import org.babyfish.kmodel.immer.runtime.metadata.ImmerType
import org.babyfish.kmodel.immer.runtime.using
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.reflect.KClass

abstract class Manager<G: Generator> {

    private val classMap: MutableMap<Class<*>, Class<*>> = WeakHashMap()

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()

    operator fun get(interfaceType: KClass<*>): Class<*> =
        this[interfaceType.java]

    operator fun get(interfaceType: Class<*>): Class<*> =
        readWriteLock.readLock().using {
            classMap[interfaceType]
        } ?: readWriteLock.writeLock().using {
            classMap[interfaceType]
                ?: HashMap<Class<*>, G>()
                    .apply {
                        createGeneratorMaps(ImmerType[interfaceType], this)
                    }
                    .run {
                        mapValues { it.value.generatedType }
                    }[interfaceType]
                ?: error("Internal bug")
        }

    private fun createGeneratorMaps(
        immerType: ImmerType,
        generatorMap: MutableMap<Class<*>, G>
    ) {
        generatorMap.computeIfAbsent(immerType.interfaceType) {
            requireDependencies(DependenciesImpl(generatorMap, immerType))
            createGenerator(immerType)
        }
    }

    protected abstract fun createGenerator(immerType: ImmerType): G

    protected open fun requireDependencies(dependencies: Dependencies) {}

    interface Dependencies {
        fun require(dependencyImmerType: ImmerType)
    }

    private inner class DependenciesImpl internal constructor(
        private val generatorMap: MutableMap<Class<*>, G>,
        private val immerType: ImmerType
    ): Dependencies {

        override fun require(immerType: ImmerType) {
            if (!classMap.containsKey(immerType.interfaceType) &&
                !generatorMap.containsKey(immerType.interfaceType)) {
                createGeneratorMaps(
                    immerType,
                    generatorMap
                )
            }
        }
    }
}