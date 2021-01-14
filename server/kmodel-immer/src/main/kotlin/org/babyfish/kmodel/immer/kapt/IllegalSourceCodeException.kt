package org.babyfish.kmodel.immer.kapt

import java.lang.RuntimeException

class IllegalSourceCodeException(message: String, cause: Throwable? = null) :
        RuntimeException(message, cause)