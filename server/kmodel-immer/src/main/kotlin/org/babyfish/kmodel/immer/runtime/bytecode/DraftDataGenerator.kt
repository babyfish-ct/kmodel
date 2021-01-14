package org.babyfish.kmodel.immer.runtime.bytecode

import jdk.internal.org.objectweb.asm.Type
import org.babyfish.kmodel.immer.kapt.DRAFT_POSTFIX
import org.babyfish.kmodel.immer.runtime.internal.ImmerDraftObject
import org.babyfish.kmodel.immer.runtime.metadata.ImmerType
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class DraftDataGenerator(
    immerType: ImmerType
) : Generator(immerType) {

    override fun generate(): Class<*> =
        ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
            .apply {
                visit(
                    Opcodes.V1_8,
                    Opcodes.ACC_PUBLIC,
                    immerType.draftDataInternalName,
                    "java/lang/Object",
                    null,
                    arrayOf(
                        "immerType.interfaceType.name$DRAFT_POSTFIX",
                        Type.getInternalName(ImmerDraftObject::class.java)
                    )
                )
                visitFields()
                visitConstructor()
            }.apply {
                visitEnd()
            }.let {
                immerType
                    .interfaceType
                    .classLoader
                    .defineClass(
                        immerType.draftDataClassName,
                        it.toByteArray()
                    )
            }

    private fun ClassVisitor.visitFields() {

    }

    private fun ClassVisitor.visitConstructor() {
        visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        ).apply {
            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
            )
            visitInsn(Opcodes.RETURN)
            visitMaxs(0, 0)
        }.visitEnd()
    }
}