package org.babyfish.kmodel.immer.runtime.bytecode

import org.babyfish.kmodel.immer.runtime.metadata.ImmerProperty
import org.babyfish.kmodel.immer.runtime.metadata.ImmerType
import org.objectweb.asm.Opcodes

abstract class Generator(
    val immerType: ImmerType
) {
    val generatedType: Class<*> by lazy {
        generate()
    }

    protected abstract fun generate(): Class<*>

    protected companion object {

        val DEFINE_CLASS_METHOD =
            ClassLoader::class.java
                .getDeclaredMethod(
                    "defineClass",
                    String::class.java,
                    ByteArray::class.java,
                    Int::class.java,
                    Int::class.java
                ).apply {
                    isAccessible = true
                }

        fun ClassLoader.defineClass(className: String, bytecode: ByteArray): Class<*> =
            DEFINE_CLASS_METHOD
                .invoke(
                    this,
                    className,
                    bytecode,
                    0,
                    bytecode.size
                ) as Class<*>

        val ImmerType.implClassName:String
            get() = "${interfaceType.name}{Impl}"

        val ImmerType.implInternalName:String
            get() = "${implClassName.replace('.', '/')}"

        val ImmerType.implDescriptor: String
            get() = "L$implInternalName;"

        val ImmerType.draftImplClassName: String
            get() = "${interfaceType.name}Draft{Impl}"

        val ImmerType.draftImplInternalName: String
            get() = draftImplClassName.replace('.', '/')

        val ImmerType.draftImplDescriptor: String
            get() = "L$draftImplInternalName;"

        val ImmerType.draftDataClassName: String
            get() = "${interfaceType.name}{DraftData}"

        val ImmerType.draftDataInternalName: String
            get() = draftDataClassName.replace('.', '/')

        val ImmerType.draftDataDescriptor: String
            get() = "L$draftDataInternalName;"

        @JvmStatic
        val ImmerProperty.stateFieldName: String
            get() = "$name{State}"

        val ImmerProperty.slotSize: Int
            get() =
                if (returnType === Long::class.java || returnType === Double::class.java) {
                    2
                } else {
                    1
                }

        val ImmerProperty.loadOpcode: Int
            get() =
                when (returnType) {
                    Boolean::class.java,
                    Char::class.java,
                    Byte::class.java,
                    Short::class.java,
                    Int::class.java -> Opcodes.ILOAD
                    Long::class.java -> Opcodes.LLOAD
                    Float::class.java -> Opcodes.FLOAD
                    Double::class.java -> Opcodes.DLOAD
                    else -> Opcodes.ALOAD
                }

        val ImmerProperty.returnOpcode: Int
            get() =
                when (returnType) {
                    Boolean::class.java,
                    Char::class.java,
                    Byte::class.java,
                    Short::class.java,
                    Int::class.java -> Opcodes.IRETURN
                    Long::class.java -> Opcodes.LRETURN
                    Float::class.java -> Opcodes.FRETURN
                    Double::class.java -> Opcodes.DRETURN
                    else -> Opcodes.ARETURN
                }
    }
}