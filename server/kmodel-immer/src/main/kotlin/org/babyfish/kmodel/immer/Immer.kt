package org.babyfish.kmodel.immer

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.annotation.JsonFilter
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@JacksonAnnotationsInside
@JsonFilter("immerPropertyFilter")
annotation class Immer
