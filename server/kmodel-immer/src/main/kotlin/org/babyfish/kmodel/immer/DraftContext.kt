package org.babyfish.kmodel.immer

interface DraftBehavior

class DraftContext<D: Any>(val draft: D) : DraftBehavior