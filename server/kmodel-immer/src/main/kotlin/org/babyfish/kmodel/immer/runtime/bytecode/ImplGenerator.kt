package org.babyfish.kmodel.immer.runtime.bytecode

import org.babyfish.kmodel.immer.runtime.PropertyState
import org.babyfish.kmodel.immer.runtime.internal.ImmerObject
import org.babyfish.kmodel.immer.runtime.metadata.ImmerProperty
import org.babyfish.kmodel.immer.runtime.metadata.ImmerType
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.*
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class ImplGenerator(
    immerType: ImmerType
): Generator(immerType) {

    override fun generate(): Class<*> =
        ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
            .apply {
                visit(
                    Opcodes.V1_8,
                    Opcodes.ACC_PUBLIC,
                    immerType.implInternalName,
                    null,
                    Type.getInternalName(Object::class.java),
                    arrayOf(
                        Type.getInternalName(immerType.interfaceType),
                        Type.getInternalName(ImmerObject::class.java)
                    )
                )
                val properties = immerType.properties
                properties.forEach {
                    visitFields(it)
                }
                visitDefaultConstructor(immerType)
                visitConstructor(immerType)
                properties.forEach {
                    visitGetter(it)
                    visitSetter(it)
                }
                visitPropertyState(immerType)
            }
            .apply {
                visitEnd()
            }.let {
                immerType
                    .interfaceType
                    .classLoader
                    .defineClass(
                        immerType.implClassName,
                        it.toByteArray()
                    )
            }

    private fun ClassVisitor.visitFields(prop: ImmerProperty) {
        visitField(
            Opcodes.ACC_PRIVATE,
            prop.stateFieldName,
            "I",
            null,
            null
        ).visitEnd()

        visitField(
            Opcodes.ACC_PRIVATE,
            prop.name,
            Type.getDescriptor(prop.returnType),
            null,
            null
        ).apply {
            if (!prop.isPrimitive && !prop.isPrimitiveBox) {
                visitAnnotation(
                    Type.getDescriptor(Nullable::class.java),
                    false
                ).visitEnd()
            }
        }.visitEnd()
    }

    private fun ClassVisitor.visitDefaultConstructor(type: ImmerType) {
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

    private fun ClassVisitor.visitConstructor(type: ImmerType) {
        visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "(${
            type.properties.map {
                "I${Type.getDescriptor(it.returnType)}"
            }.joinToString("")
            })V",
            null,
            null
        ).apply {
            visitCode()
            for (index in 0 until type.properties.size) {
                val prop = type.properties[index]
                if (!prop.isPrimitive && !prop.isPrimitiveBox) {
                    visitParameterAnnotation(
                        (index * 2 + 1),
                        Type.getDescriptor(Nullable::class.java),
                        false
                    ).visitEnd()
                }
            }
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
            )
            var slot:Int = 1
            type.properties.forEach {

                visitVarInsn(Opcodes.ALOAD, 0)
                visitVarInsn(Opcodes.ILOAD, slot++)
                visitFieldInsn(
                    Opcodes.PUTFIELD,
                    type.implInternalName,
                    it.stateFieldName,
                    "I"
                )

                visitVarInsn(Opcodes.ALOAD, 0)
                visitVarInsn(it.loadOpcode, slot)
                visitFieldInsn(
                    Opcodes.PUTFIELD,
                    type.implInternalName,
                    it.name,
                    Type.getDescriptor(it.returnType)
                )
                slot += it.slotSize
            }
            visitInsn(Opcodes.RETURN)
            visitMaxs(0, 0)
        }.visitEnd()
    }

    private fun ClassVisitor.visitGetter(prop: ImmerProperty) {
        visitMethod(
            Opcodes.ACC_PUBLIC,
            prop.getter.name,
            Type.getMethodDescriptor(prop.getter),
            null,
            null
        ).apply {

            val validStateLabel = Label()

            if (!prop.isPrimitive && !prop.isPrimitiveBox) {
                if (prop.nullable) {
                    visitAnnotation(
                        Type.getDescriptor(Nullable::class.java),
                        false
                    ).visitEnd()
                } else {
                    visitAnnotation(
                        Type.getDescriptor(NotNull::class.java),
                        false
                    ).visitEnd()
                }
            }

            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitFieldInsn(
                Opcodes.GETFIELD,
                prop.declaredImmerType.implInternalName,
                prop.stateFieldName,
                "I"
            )
            visitInsn(Opcodes.ICONST_2)
            visitJumpInsn(Opcodes.IF_ICMPEQ, validStateLabel)
            visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalStateException::class.java))
            visitInsn(Opcodes.DUP)
            visitLdcInsn("The property ${prop.name} is not fetched")
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                Type.getInternalName(IllegalStateException::class.java),
                "<init>",
                "(Ljava/lang/String;)V",
                false
            )
            visitInsn(Opcodes.ATHROW)

            visitLabel(validStateLabel)
            visitVarInsn(Opcodes.ALOAD, 0)
            visitFieldInsn(
                Opcodes.GETFIELD,
                prop.declaredImmerType.implInternalName,
                prop.name,
                Type.getDescriptor(prop.returnType)
            )
            visitInsn(prop.returnOpcode)
            visitMaxs(0, 0)
        }.visitEnd()
    }

    private fun ClassVisitor.visitSetter(prop: ImmerProperty) {
        visitMethod(
            Opcodes.ACC_PUBLIC,
            prop.name.let {
                "set${it[0].toUpperCase()}${it.substring(1)}"
            },
            "(${Type.getDescriptor(prop.returnType)})V",
            null,
            null
        ).apply {

            if (!prop.isPrimitive && !prop.isPrimitiveBox) {
                if (prop.nullable) {
                    visitParameterAnnotation(
                        0,
                        Type.getDescriptor(Nullable::class.java),
                        false
                    ).visitEnd()
                } else {
                    visitParameterAnnotation(
                        0,
                        Type.getDescriptor(NotNull::class.java),
                        false
                    ).visitEnd()
                }
            }

            visitCode()

            visitVarInsn(Opcodes.ALOAD, 0)
            visitInsn(Opcodes.ICONST_2)
            visitFieldInsn(
                Opcodes.PUTFIELD,
                immerType.implInternalName,
                prop.stateFieldName,
                "I"
            )

            visitVarInsn(Opcodes.ALOAD, 0)
            visitVarInsn(prop.loadOpcode, 1)
            visitFieldInsn(
                Opcodes.PUTFIELD,
                immerType.implInternalName,
                prop.name,
                Type.getDescriptor(prop.returnType)
            )

            visitMaxs(0, 0)

            visitInsn(Opcodes.RETURN)
            visitEnd()
        }
    }

    private fun ClassVisitor.visitPropertyState(immerType: ImmerType) {
        visitMethod(
            Opcodes.ACC_PUBLIC,
            "propertyState",
            "(Ljava/lang/String;)${Type.getDescriptor(PropertyState::class.java)}",
            null,
            null
        ).apply {
            visitParameterAnnotation(
                0,
                Type.getDescriptor(NotNull::class.java),
                false
            ).visitEnd()
            visitAnnotation(
                Type.getDescriptor(NotNull::class.java),
                false
            ).visitEnd()
            visitCode()
//            visitVarInsn(Opcodes.ALOAD, 1)
//            visitMethodInsn(
//                Opcodes.INVOKEVIRTUAL,
//                "java/lang/String",
//                "hashCode",
//                "()I",
//                false
//            )
//            val hashCodes = immerType.properties.map { it.name.hashCode() }.toIntArray()
//            val labels = hashCodes.map { Label() }.toTypedArray()
//            val defaultLabel = Label()
//            visitLookupSwitchInsn(
//                defaultLabel,
//                hashCodes,
//                labels
//            )
//            for (i in 0 until hashCodes.size) {
//                visitLabel(labels[i])
//                visitVarInsn(Opcodes.ALOAD, 1)
//                visitLdcInsn(immerType.properties[i].name)
//                visitMethodInsn(
//                    Opcodes.INVOKEVIRTUAL,
//                    "java/lang/String",
//                    "equals",
//                    "(Ljava/lang/Object;)Z",
//                    false
//                )
//                visitJumpInsn(Opcodes.IFEQ, defaultLabel)
//                visitVarInsn(Opcodes.ALOAD, 0)
//                visitFieldInsn(
//                    Opcodes.GETFIELD,
//                    immerType.implInternalName,
//                    "${immerType.properties[i].name}{State}",
//                    "I"
//                )
//                visitReturnAsPropertyState()
//            }
//            visitLabel(defaultLabel)
            visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException::class.java))
            visitInsn(Opcodes.DUP)
            visitLdcInsn("Illegal property name is specified")
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                Type.getInternalName(IllegalArgumentException::class.java),
                "<init>",
                "(Ljava/lang/String;)V",
                false
            )
            visitInsn(Opcodes.ATHROW)
            visitMaxs(0, 0)
        }.visitEnd()
    }

    private fun MethodVisitor.visitReturnAsPropertyState() {
//        val labels = (0..2).map { Label() }
//        visitTableSwitchInsn(0, 1, labels[2], labels[0], labels[1])
//        for (i in 0 until labels.size) {
//            visitLabel(labels[i])
//            visitFieldInsn(
//                Opcodes.GETSTATIC,
//                Type.getInternalName(PropertyState::class.java),
//                PropertyState.values()[i].name,
//                Type.getDescriptor(PropertyState::class.java)
//            )
//            visitInsn(Opcodes.ARETURN)
//        }
        visitInsn(Opcodes.ICONST_0)
        visitInsn(Opcodes.ARETURN)
    }
}