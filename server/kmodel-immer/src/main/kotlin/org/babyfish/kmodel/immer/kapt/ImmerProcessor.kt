package org.babyfish.kmodel.immer.kapt

import com.squareup.kotlinpoet.asTypeName
import org.babyfish.kmodel.immer.Immer
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("org.babyfish.kmodel.immer.Immer")
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ImmerProcessor : AbstractProcessor() {

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment
    ): Boolean {
        val kaptContext = KaptContext()
        roundEnv.rootElements.forEach {
            val packageName = processingEnv
                .elementUtils
                .getPackageOf(it)
                .toString()
            kaptContext.addElement(packageName, it)
        }
        try {
            roundEnv
                .getElementsAnnotatedWith(Immer::class.java)
                .forEach {
                    kaptContext[it.asType().asTypeName().toClassName()]!!
                        .generate()
                        .writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]))
                }
        } catch (ex: IllegalSourceCodeException) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                ex.message
            )
            return false
        }
        return true
    }
}

