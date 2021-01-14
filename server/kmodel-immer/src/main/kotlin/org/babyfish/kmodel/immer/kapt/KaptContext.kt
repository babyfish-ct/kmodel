package org.babyfish.kmodel.immer.kapt

import com.squareup.kotlinpoet.ClassName
import org.babyfish.kmodel.immer.Immer
import org.babyfish.kmodel.immer.runtime.lockedComputeIfAbsent
import org.babyfish.kmodel.immer.runtime.using
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

class KaptContext {

    private val readWriteLock = ReentrantReadWriteLock()

    private val classImmerMap = HashMap<ClassName, ImmerResult>()

    fun addElement(packageName: String, element: Element) {
        readWriteLock.writeLock().using {
            writeImpl(packageName, element as TypeElement)
        }
    }

    val immerTypes: List<ImmerType> =
        readWriteLock.readLock().using {
            classImmerMap.values.mapNotNull { it.immerType }
        }

    operator fun get(className: ClassName): ImmerType? =
        className
            .let {
                if (it.nullable) {
                    it.asNonNullable()
                } else {
                    it
                }
            }
            .let {
                classImmerMap.lockedComputeIfAbsent(readWriteLock, it) { key ->
                    determineImmerResult(key)
                }.immerType
            }

    private fun writeImpl(packageName: String, typeElement: TypeElement) {
        classImmerMap[ClassName(packageName, typeElement.simpleName.toString())] =
            ImmerResult(
                typeElement
                    .takeIf { it.getAnnotation(Immer::class.java) !== null }
                    ?.let { ImmerType(this, packageName, it) }
            )
        typeElement.enclosedElements.forEach {
            if (it is TypeElement) {
                writeImpl(packageName, it)
            }
        }
    }

    private fun determineImmerResult(className: ClassName): ImmerResult {
        if (className.packageName() == "kotlin") {
            return ImmerResult()
        }
        if (className.packageName() == "kotlin.collections") {
            return ImmerResult()
        }
        return ImmerResult(
            Class
                .forName(className.toString())
                .takeIf { it.isAnnotationPresent(Immer::class.java) }
                ?.let { ImmerType(this, it) }
        )
    }

    private data class ImmerResult(
        val immerType: ImmerType? = null
    )
}