package org.babyfish.kmodel.seata

import io.seata.rm.datasource.undo.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KProperty0
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible

private val hackedDbTypes = mutableSetOf<String>()

private val hackedDbTypeLock = ReentrantReadWriteLock()

internal fun hack(dbType: String) {
    hackedDbTypeLock.read {
        if (hackedDbTypes.contains(dbType)) {
            return
        }
    }
    hackedDbTypeLock.write {
        if (hackedDbTypes.contains(dbType)) {
            return
        }
        hackUndoLogManager(dbType)
        hackUndoExecutorHolder(dbType)
        hackedDbTypes += dbType
    }
}

private fun hackUndoLogManager(dbType: String) {
    val undoLogManager =
        UndoLogManagerFactory.getUndoLogManager(dbType)
    if (undoLogManager is UndoLogManagerProxy) {
        return
    }
    val proxy = UndoLogManagerProxy(undoLogManager)
    SEATA_UNDO_LOG_MANAGER_MAP.get()[dbType] = proxy
}

private fun hackUndoExecutorHolder(dbType: String) {
    val undoExecutorHolder =
        UndoExecutorHolderFactory.getUndoExecutorHolder(dbType)
    if (undoExecutorHolder is UndoExecutorHolderProxy) {
        return
    }
    val proxy = UndoExecutorHolderProxy(undoExecutorHolder)
    SEATA_UNDO_EXECUTOR_HOLDER_MAP.get()[dbType] = proxy
}

private val SEATA_UNDO_LOG_MANAGER_MAP =
    UndoLogManagerProxy::class.staticProperties
        .firstOrNull {
            it.name == "UNDO_LOG_MANAGER_MAP"
        }
        ?.also {
            it.isAccessible = true
        }
        as KProperty0<MutableMap<String, UndoLogManager>>?
        ?: error(
            "No static field ${UndoLogManagerProxy::class}" +
                    ".UNDO_LOG_MANAGER_MAP"
        )

private val SEATA_UNDO_EXECUTOR_HOLDER_MAP =
    UndoExecutorHolderFactory::class.staticProperties
        .firstOrNull {
            it.name == "UNDO_EXECUTOR_HOLDER_MAP"
        }
        ?.also {
            it.isAccessible = true
        }
        as KProperty0<MutableMap<String, UndoExecutorHolder>>?
        ?: error(
            "No static field ${UndoExecutorHolderProxy::class}" +
                    ".UNDO_EXECUTOR_HOLDER_MAP"
        )