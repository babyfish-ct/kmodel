package org.babyfish.kmodel.immer.runtime

import org.babyfish.kmodel.immer.runtime.bytecode.ImplManager
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.reflect.KClass

object ImmerRuntime {

    private val implInstanceMap: Map<KClass<*>, Any> =
        HashMap()

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()

    fun <T: Any> get(type: KClass<T>): T =
        readWriteLock.readLock().using {
            implInstanceMap[type] as T?
        } ?: readWriteLock.writeLock().using {
            implInstanceMap[type] as T?
                ?: ImplManager[type].newInstance() as T
        }

    fun <D: Any> newDraft(draftType: KClass<D>): D = TODO()

    fun <T: Any, D: T> modifyReference(
        draftType: KClass<D>,
        baseState: T,
        draftHandler: (D) -> Unit
    ): T = TODO()

    fun <E: Any, D: E> modifyList(
        draftType: KClass<D>,
        baseList: List<E>,
        draftHandler: (MutableList<D>) -> Unit
    ): List<E> = TODO()
}