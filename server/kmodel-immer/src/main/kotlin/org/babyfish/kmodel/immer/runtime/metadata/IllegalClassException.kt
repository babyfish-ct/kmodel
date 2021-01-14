package org.babyfish.kmodel.immer.runtime.metadata

import java.lang.RuntimeException

class IllegalClassException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)