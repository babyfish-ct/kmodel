package org.babyfish.kmodel.immer.runtime.internal

class DraftList<D: E, E>(
    private val parent: ImmerDraftObject<*>,
    private val list: List<E>,
    private val elementDraftChecker: (E) -> Boolean,
    private val elementDraftConverter: (E) -> D
) : MutableList<D>, ImmerDraft<List<E>> {

    private var changed: Boolean = false

    private var modCount: Int = 0

    private var overrideList: MutableList<D>? =
        if (list is MutableList && list.filter(elementDraftChecker).size == list.size) {
            null
        } else {
            nonNullOverrideList
        }

    private val nonNullOverrideList: MutableList<D>
        get() =
            list
                .map {
                    if (elementDraftChecker(it)) {
                        it as D
                    } else {
                        elementDraftConverter(it)
                    }
                }
                .toMutableList()

    override fun isEmpty(): Boolean =
        (overrideList ?: list).isEmpty()

    override val size: Int
        get() = (overrideList ?: list).size

    override fun contains(element: D): Boolean =
        (overrideList ?: list).contains(element)

    override fun containsAll(elements: Collection<D>): Boolean =
        (overrideList ?: list).containsAll(elements)

    override fun get(index: Int): D =
        overrideList.let {
            if (it !== null) {
                it[index]
            } else {
                list[index] as D
            }
        }

    override fun indexOf(element: D): Int =
        (overrideList ?: list).indexOf(element)

    override fun lastIndexOf(element: D): Int =
        (overrideList ?: list).lastIndexOf(element)

    override fun iterator(): MutableIterator<D> = TODO()

    override fun listIterator(): MutableListIterator<D> = TODO()

    override fun listIterator(index: Int): MutableListIterator<D> = TODO()

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<D> = TODO()

    override fun add(element: D): Boolean {
        markChanged()
        modCount++
        return nonNullOverrideList.add(element)
    }

    override fun add(index: Int, element: D) {
        markChanged()
        modCount++
        return nonNullOverrideList.add(index, element)
    }

    override fun addAll(elements: Collection<D>): Boolean {
        markChanged()
        modCount++
        return nonNullOverrideList.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<D>): Boolean {
        markChanged()
        modCount++
        return nonNullOverrideList.addAll(index, elements)
    }

    override fun clear() {
        markChanged()
        modCount++
        nonNullOverrideList.clear()
    }

    override fun remove(element: D): Boolean {
        val index = indexOf(element)
        if (index != -1) {
            markChanged()
            modCount++
            nonNullOverrideList.removeAt(index)
            return true
        }
        return false
    }

    override fun removeAll(elements: Collection<D>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAt(index: Int): D {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retainAll(elements: Collection<D>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun set(index: Int, element: D): D {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hashCode(): Int =
        (overrideList ?: list).hashCode()

    override fun equals(other: Any?): Boolean =
        (overrideList ?: list) == other

    override fun toString(): String {
        return (overrideList ?: list).toString()
    }

    override val isChanged: Boolean
        get() = changed

    override fun markChanged() {
        changed = true
        parent?.markChanged()
    }

    override fun toImmer(): List<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}