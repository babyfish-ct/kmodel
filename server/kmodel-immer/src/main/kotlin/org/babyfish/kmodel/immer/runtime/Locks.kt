package org.babyfish.kmodel.immer.runtime

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock

fun <T> Lock.using(handler: () -> T): T {
    lock()
    try {
        return handler()
    } finally {
        unlock()
    }
}

/*
 * Be different with ConcurrentHashMap,
 * the lock scope is controlled by you when you are not using this function.
 */
fun <K, V: Any> MutableMap<K, V>.lockedComputeIfAbsent(
    readWriteLock: ReadWriteLock,
    key: K,
    valueCreator: (K) -> V
): V =
    readWriteLock.readLock().using {
        this[key]
    } ?: readWriteLock.writeLock().using {
        this[key]
            ?: valueCreator(key).also {
                this[key] = it
            }
    }