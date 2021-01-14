package org.babyfish.kmodel.immer.runtime.bytecode

import jdk.internal.org.objectweb.asm.Type
import org.babyfish.kmodel.immer.kapt.DRAFT_POSTFIX
import org.babyfish.kmodel.immer.runtime.internal.ImmerDraftObject
import org.babyfish.kmodel.immer.runtime.metadata.ImmerType
import org.jetbrains.annotations.NotNull
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class DraftGenerator(
    immerType: ImmerType
) : Generator(immerType) {

    override fun generate(): Class<*> =
        ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
            .apply {
                visit(
                    Opcodes.V1_8,
                    Opcodes.ACC_PUBLIC,
                    immerType.draftImplInternalName,
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
                        immerType.draftImplClassName,
                        it.toByteArray()
                    )
            }

    private fun ClassVisitor.visitFields() {
        visitField(
            Opcodes.ACC_PRIVATE,
            "{parent}",
            Type.getDescriptor(immerType.interfaceType),
            null,
            null
        ).apply {
            visitAnnotation(
                Type.getDescriptor(NotNull::class.java),
                false
            ).visitEnd()
        }.visitEnd()

        visitField(
            Opcodes.ACC_PRIVATE,
            "{changed}",
            "Z",
            null,
            null
        ).visitEnd()

        immerType.properties.forEach {
            visitField(
                Opcodes.ACC_PRIVATE,
                it.name,
                Type.getDescriptor(it.returnType),
                null,
                null
            ).apply {
                if (!it.isPrimitive && !it.isPrimitiveBox) {
                    visitAnnotation(
                        Type.getDescriptor(NotNull::class.java),
                        false
                    ).visitEnd()
                }
            }.visitEnd()
            visitField(
                Opcodes.ACC_PRIVATE,
                "${it.name}{StateOrdinal}",
                "I",
                null,
                null
            ).visitEnd()
        }
    }

    private fun ClassVisitor.visitConstructor() {
        visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "(${Type.getDescriptor(immerType.interfaceType)})V",
            null,
            null
        ).apply {
            visitParameterAnnotation(
                0,
                Type.getDescriptor(NotNull::class.java),
                false
            ).visitEnd()
            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitVarInsn(Opcodes.ALOAD, 1)
            visitFieldInsn(
                Opcodes.PUTFIELD,
                immerType.draftImplInternalName,
                "{parent}",
                immerType.implDescriptor
            )
            visitInsn(Opcodes.RETURN)
            visitMaxs(0, 0)
        }.visitEnd()
    }
}