package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.*
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*


abstract class MixinExtrasInjectorAnnotationHandler : InjectorAnnotationHandler() {
    open val oldSuperBehaviour = false

    enum class ElementType {
        METHOD_CALL {
            override fun matches(insn: AbstractInsnNode) = insn is MethodInsnNode && insn.name != "<init>"
        },
        FIELD_GET {
            override fun matches(insn: AbstractInsnNode) = insn.opcode == Opcodes.GETFIELD || insn.opcode == Opcodes.GETSTATIC
        },
        FIELD_SET {
            override fun matches(insn: AbstractInsnNode) = insn.opcode == Opcodes.PUTFIELD || insn.opcode == Opcodes.PUTSTATIC
        },
        INSTANTIATION {
            override fun matches(insn: AbstractInsnNode) = insn.opcode == Opcodes.NEW
        },
        INSTANCEOF {
            override fun matches(insn: AbstractInsnNode) = insn.opcode == Opcodes.INSTANCEOF
        },
        CONSTANT {
            override fun matches(insn: AbstractInsnNode) = isConstant(insn)
        },
        RETURN {
            override fun matches(insn: AbstractInsnNode) = insn.opcode in Opcodes.IRETURN..Opcodes.ARETURN
        };

        abstract fun matches(insn: AbstractInsnNode): Boolean
    }

    abstract val supportedElementTypes: Collection<ElementType>

    open fun extraTargetRestrictions(insn: AbstractInsnNode): Boolean = true

    abstract fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: AbstractInsnNode
    ): Pair<ParameterGroup, PsiType>?

    override val allowCoerce = true

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<MethodSignature>? {
        val insns = resolveInstructions(annotation, targetClass, targetMethod).ifEmpty { return emptyList() }.map { it.insn }
        if (insns.any { insn -> supportedElementTypes.none { it.matches(insn) } }) return emptyList()
        val signatures = insns.map { expectedMethodSignature(annotation, targetClass, targetMethod, it) }
        val firstMatch = signatures[0] ?: return emptyList()
        if (signatures.drop(1).any { it != firstMatch }) return emptyList()
        return listOf(
            MethodSignature(
                listOf(
                    firstMatch.first,
                    ParameterGroup(
                        collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                        required = ParameterGroup.RequiredLevel.OPTIONAL,
                        isVarargs = true,
                    ),
                ),
                firstMatch.second
            )
        )
    }

    protected fun getInsnReturnType(insn: AbstractInsnNode): Type? {
        return when {
            insn is MethodInsnNode -> Type.getReturnType(insn.desc)
            insn is FieldInsnNode -> when (insn.opcode) {
                Opcodes.GETFIELD, Opcodes.GETSTATIC -> Type.getType(insn.desc)
                else -> Type.VOID_TYPE
            }
            insn is TypeInsnNode -> when (insn.opcode) {
                Opcodes.NEW -> Type.getObjectType(insn.desc)
                Opcodes.INSTANCEOF -> Type.BOOLEAN_TYPE
                else -> null
            }
            isConstant(insn) -> getConstantType(insn)
            else -> null
        }
    }

    protected fun getPsiReturnType(
        insn: AbstractInsnNode,
        annotation: PsiAnnotation
    ): PsiType? {
        val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

        return when (insn) {
            is MethodInsnNode -> {
                val sourceClassAndMethod = (
                        MemberReference(insn.name, insn.desc, insn.owner.replace('/', '.'))
                            .resolveAsm(annotation.project) as? MethodTargetMember
                        )?.classAndMethod
                sourceClassAndMethod?.method?.getGenericReturnType(sourceClassAndMethod.clazz, annotation.project)
                    ?: Type.getType(insn.desc).toPsiType(elementFactory)
            }
            is FieldInsnNode -> {
                val sourceClassAndField = (
                        MemberReference(insn.name, insn.desc, insn.owner.replace('/', '.'))
                            .resolveAsm(annotation.project) as? FieldTargetMember
                        )?.classAndField
                sourceClassAndField?.field?.getGenericType(sourceClassAndField.clazz, annotation.project)
                    ?: Type.getType(insn.desc).toPsiType(elementFactory)
            }
            else -> getInsnReturnType(insn)?.toPsiType(elementFactory)
        }
    }

    protected fun getInsnArgTypes(
        insn: AbstractInsnNode,
        targetClass: ClassNode
    ): List<Type>? {
        return when (insn) {
            is MethodInsnNode -> {
                val args = Type.getArgumentTypes(insn.desc).toMutableList()
                when (insn.opcode) {
                    Opcodes.INVOKESTATIC -> {}
                    Opcodes.INVOKESPECIAL -> {
                        args.add(0, Type.getObjectType(if (oldSuperBehaviour) insn.owner else targetClass.name))
                    }
                    else -> {
                        args.add(0, Type.getObjectType(insn.owner))
                    }
                }
                args
            }
            is FieldInsnNode -> {
                when (insn.opcode) {
                    Opcodes.GETFIELD -> listOf(Type.getObjectType(insn.owner))
                    Opcodes.PUTFIELD -> listOf(Type.getObjectType(insn.owner), Type.getType(insn.desc))
                    Opcodes.GETSTATIC -> emptyList()
                    Opcodes.PUTSTATIC -> listOf(Type.getType(insn.desc))
                    else -> null
                }
            }
            is TypeInsnNode -> {
                when (insn.opcode) {
                    Opcodes.INSTANCEOF -> listOf(Type.getType(Any::class.java))
                    else -> null
                }
            }
            else -> null
        }
    }

    protected fun getPsiParameters(
        insn: AbstractInsnNode,
        targetClass: ClassNode,
        annotation: PsiAnnotation
    ): List<Parameter>? {
        val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

        return when (insn) {
            is MethodInsnNode -> {
                val sourceClassAndMethod = (
                        MemberReference(insn.name, insn.desc, insn.owner.replace('/', '.'))
                            .resolveAsm(annotation.project) as? MethodTargetMember
                        )?.classAndMethod
                val parameters = mutableListOf<Parameter>()
                if (insn.opcode != Opcodes.INVOKESTATIC) {
                    val receiver = if (insn.opcode == Opcodes.INVOKESPECIAL && !oldSuperBehaviour) targetClass.name else insn.owner
                    parameters += Parameter("instance", Type.getObjectType(receiver).toPsiType(elementFactory))
                }
                val genericParams = sourceClassAndMethod?.method?.getGenericParameterTypes(
                    sourceClassAndMethod.clazz,
                    annotation.project,
                )
                if (genericParams != null) {
                    genericParams.withIndex().mapTo(parameters) { (index, type) ->
                        val i = if (insn.opcode == Opcodes.INVOKESTATIC) index else index + 1
                        val name = sourceClassAndMethod.method.localVariables?.getOrNull(i)?.name?.toJavaIdentifier()
                        sanitizedParameter(type, name)
                    }
                } else {
                    Type.getArgumentTypes(insn.desc).mapTo(parameters) {
                        sanitizedParameter(it.toPsiType(elementFactory), null)
                    }
                }
                parameters
            }
            is FieldInsnNode -> {
                val ownerType = Type.getObjectType(insn.owner).toPsiType(elementFactory)
                val sourceClassAndField = (
                        MemberReference(insn.name, insn.desc, insn.owner.replace('/', '.'))
                            .resolveAsm(annotation.project) as? FieldTargetMember
                        )?.classAndField
                val valueType = sourceClassAndField?.field?.getGenericType(sourceClassAndField.clazz, annotation.project)
                    ?: Type.getType(insn.desc).toPsiType(elementFactory)
                val owner = Parameter("instance", ownerType)
                val value = Parameter("value", valueType)
                when (insn.opcode) {
                    Opcodes.GETFIELD -> listOf(owner)
                    Opcodes.PUTFIELD -> listOf(owner, value)
                    Opcodes.GETSTATIC -> emptyList()
                    Opcodes.PUTSTATIC -> listOf(value)
                    else -> null
                }
            }
            is TypeInsnNode -> {
                when (insn.opcode) {
                    Opcodes.INSTANCEOF -> listOf(Parameter("object", Type.getType(Any::class.java).toPsiType(elementFactory)))
                    else -> null
                }
            }
            else -> null
        } ?: getInsnArgTypes(insn, targetClass)?.map { Parameter(null, it.toPsiType(elementFactory)) }
    }
}

private val CONSTANTS_ALL = intArrayOf(
    Opcodes.ACONST_NULL,
    Opcodes.ICONST_M1,
    Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5,
    Opcodes.LCONST_0, Opcodes.LCONST_1,
    Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2,
    Opcodes.DCONST_0, Opcodes.DCONST_1,
    Opcodes.BIPUSH,  // 15
    Opcodes.SIPUSH,  // 16
    Opcodes.LDC,  // 17
    Opcodes.CHECKCAST,  // 18
    Opcodes.INSTANCEOF // 19
)

private val CONSTANTS_TYPES = arrayOf(
    "V",  // null is returned as Type.VOID_TYPE
    "I",
    "I", "I", "I", "I", "I", "I",
    "J", "J",
    "F", "F", "F",
    "D", "D",
    "I",  //"B",
    "I"
)

private fun isConstant(insn: AbstractInsnNode?): Boolean {
    return insn != null && insn.opcode in CONSTANTS_ALL
}

private fun getConstantType(insn: AbstractInsnNode?): Type? {
    return when (insn) {
        null -> null
        is LdcInsnNode -> {
            val cst = insn.cst
            when (cst) {
                is Int -> return Type.INT_TYPE
                is Float -> return Type.FLOAT_TYPE
                is Long -> return Type.LONG_TYPE
                is Double -> return Type.DOUBLE_TYPE
                is String -> return Type.getType(String::class.java)
                is Type -> return Type.getType(Class::class.java)
                else -> null
            }
        }
        is TypeInsnNode -> {
            return if (insn.getOpcode() < Opcodes.CHECKCAST) {
                null // Don't treat NEW and ANEWARRAY as constants
            } else Type.getType(Class::class.java)
        }
        else -> {
            val index = CONSTANTS_ALL.indexOf(insn.opcode)
            return if (index < 0) null else Type.getType(CONSTANTS_TYPES.get(index))
        }
    }
}