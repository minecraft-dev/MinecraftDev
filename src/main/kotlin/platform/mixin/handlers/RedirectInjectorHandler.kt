/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.NewInsnInjectionPoint
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.AsmDfaUtil
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.getGenericSignature
import com.demonwav.mcdev.platform.mixin.util.getGenericType
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

class RedirectInjectorHandler : InjectorAnnotationHandler() {
    private fun getRedirectType(insn: AbstractInsnNode): RedirectType? {
        return when (insn) {
            is FieldInsnNode -> {
                if (insn.opcode == Opcodes.GETSTATIC || insn.opcode == Opcodes.GETFIELD) {
                    FieldGet
                } else {
                    FieldSet
                }
            }
            is MethodInsnNode -> {
                Method
            }
            is InsnNode -> when (insn.opcode) {
                Opcodes.ARRAYLENGTH -> ArrayLength
                in Opcodes.IALOAD..Opcodes.SALOAD -> ArrayGet
                in Opcodes.IASTORE..Opcodes.SASTORE -> ArraySet
                else -> null
            }
            is TypeInsnNode -> when (insn.opcode) {
                Opcodes.NEW -> Constructor
                Opcodes.INSTANCEOF -> InstanceOf
                else -> null
            }
            else -> null
        }
    }

    override fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return getRedirectType(insn)?.isInsnAllowed(insn) ?: false
    }

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<MethodSignature>? {
        val insns = resolveInstructions(annotation, targetClass, targetMethod).ifEmpty { return emptyList() }
        return getRedirectType(insns[0].insn)?.expectedMethodSignature(
            annotation,
            targetClass,
            targetMethod,
            insns.map { it.insn }
        )?.map { (paramGroups, returnType) ->
            // add a parameter group for capturing the target method parameters
            val extraGroup = ParameterGroup(
                collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                required = ParameterGroup.RequiredLevel.OPTIONAL,
                isVarargs = true
            )
            MethodSignature(paramGroups + extraGroup, returnType)
        }
    }

    override val allowCoerce = true

    private interface RedirectType {
        fun isInsnAllowed(node: AbstractInsnNode) = true

        fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature>?
    }

    private object FieldGet : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature> {
            val firstMatch = insns.first() as FieldInsnNode
            val isValid = insns.all {
                val insn = it as? FieldInsnNode ?: return@all false
                if (insn.opcode != firstMatch.opcode) return@all false
                if (insn.opcode == Opcodes.GETFIELD && insn.owner != firstMatch.owner) return@all false
                insn.name == firstMatch.name && insn.desc == firstMatch.desc
            }
            if (!isValid) {
                return emptyList()
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val sourceClassAndField = (
                MemberReference(firstMatch.name, firstMatch.desc, firstMatch.owner.replace('/', '.'))
                    .resolveAsm(annotation.project) as? FieldTargetMember
                )?.classAndField
            val fieldType = sourceClassAndField?.field?.getGenericType(sourceClassAndField.clazz, annotation.project)
                ?: Type.getType(firstMatch.desc).toPsiType(elementFactory)

            val parameters = mutableListOf<Parameter>()
            if (firstMatch.opcode == Opcodes.GETFIELD) {
                parameters += Parameter("instance", Type.getObjectType(firstMatch.owner).toPsiType(elementFactory))
            }

            return listOf(
                MethodSignature(
                    listOf(ParameterGroup(parameters)),
                    fieldType
                )
            )
        }
    }

    private object FieldSet : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature> {
            val firstMatch = insns.first() as FieldInsnNode
            val isValid = insns.all {
                val insn = it as? FieldInsnNode ?: return@all false
                if (insn.opcode != firstMatch.opcode) return@all false
                if (insn.opcode == Opcodes.PUTFIELD && insn.owner != firstMatch.owner) return@all false
                insn.name == firstMatch.name && insn.desc == firstMatch.desc
            }
            if (!isValid) {
                return emptyList()
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val sourceClassAndField = (
                MemberReference(firstMatch.name, firstMatch.desc, firstMatch.owner.replace('/', '.'))
                    .resolveAsm(annotation.project) as? FieldTargetMember
                )?.classAndField
            val fieldType = sourceClassAndField?.field?.getGenericType(sourceClassAndField.clazz, annotation.project)
                ?: Type.getType(firstMatch.desc).toPsiType(elementFactory)

            val parameters = mutableListOf<Parameter>()
            if (firstMatch.opcode == Opcodes.PUTFIELD) {
                parameters += Parameter("instance", Type.getObjectType(firstMatch.owner).toPsiType(elementFactory))
            }
            parameters += Parameter("value", fieldType)

            return listOf(
                MethodSignature(
                    listOf(ParameterGroup(parameters)),
                    PsiType.VOID
                )
            )
        }
    }

    private object Method : RedirectType {
        override fun isInsnAllowed(node: AbstractInsnNode): Boolean {
            return (node as MethodInsnNode).name != "<init>"
        }

        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature> {
            val firstMatch = insns.first() as MethodInsnNode
            val isValid = insns.all {
                val insn = it as? MethodInsnNode ?: return@all false
                if ((insn.opcode == Opcodes.INVOKESTATIC) != (firstMatch.opcode == Opcodes.INVOKESTATIC)) {
                    return@all false
                }
                if (insn.opcode != Opcodes.INVOKESTATIC && insn.owner != firstMatch.owner) return@all false
                insn.name == firstMatch.name && insn.desc == firstMatch.desc
            }
            if (!isValid) {
                return emptyList()
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val sourceClassAndMethod = (
                MemberReference(firstMatch.name, firstMatch.desc, firstMatch.owner.replace('/', '.'))
                    .resolveAsm(annotation.project) as? MethodTargetMember
                )?.classAndMethod
            val signature = sourceClassAndMethod?.method?.getGenericSignature(
                sourceClassAndMethod.clazz,
                annotation.project
            )

            val parameters = mutableListOf<Parameter>()
            if (firstMatch.opcode != Opcodes.INVOKESTATIC) {
                parameters += Parameter("instance", Type.getObjectType(firstMatch.owner).toPsiType(elementFactory))
            }

            if (signature != null) {
                signature.second
                    .asSequence()
                    .withIndex()
                    .mapTo(parameters) { (index, type) ->
                        val i = if (firstMatch.opcode == Opcodes.INVOKESTATIC) index else index + 1
                        val name = sourceClassAndMethod.method.localVariables?.getOrNull(i)?.name?.toJavaIdentifier()
                        sanitizedParameter(type, name)
                    }
            } else {
                Type.getArgumentTypes(firstMatch.desc).mapTo(parameters) {
                    sanitizedParameter(it.toPsiType(elementFactory), null)
                }
            }

            val returnType = signature?.first ?: Type.getReturnType(firstMatch.desc).toPsiType(elementFactory)
            return listOf(MethodSignature(listOf(ParameterGroup(parameters)), returnType))
        }
    }

    private object ArrayLength : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature>? {
            val firstMatch = insns.first()
            val isValid = insns.all { it.opcode == firstMatch.opcode }
            if (!isValid) return emptyList()

            val arrayType = AsmDfaUtil.getStackType(annotation.project, targetClass, targetMethod, firstMatch, 0)
                ?: return null
            if (arrayType.sort != Type.ARRAY) {
                return null
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            return listOf(
                MethodSignature(
                    listOf(
                        ParameterGroup(
                            listOf(
                                Parameter("array", arrayType.toPsiType(elementFactory))
                            )
                        )
                    ),
                    PsiType.INT
                )
            )
        }
    }

    private object ArrayGet : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature>? {
            val firstMatch = insns.first()
            val isValid = insns.all { it.opcode == firstMatch.opcode }
            if (!isValid) return emptyList()

            val arrayType = AsmDfaUtil.getStackType(annotation.project, targetClass, targetMethod, firstMatch, 1)
                ?: return null
            if (arrayType.sort != Type.ARRAY) {
                return null
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val psiArrayType = arrayType.toPsiType(elementFactory) as PsiArrayType
            return listOf(
                MethodSignature(
                    listOf(
                        ParameterGroup(
                            listOf(
                                Parameter("array", psiArrayType),
                                Parameter("index", PsiType.INT)
                            )
                        )
                    ),
                    psiArrayType.componentType
                )
            )
        }
    }

    private object ArraySet : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature>? {
            val firstMatch = insns.first()
            val isValid = insns.all { it.opcode == firstMatch.opcode }
            if (!isValid) return emptyList()

            val arrayType = AsmDfaUtil.getStackType(annotation.project, targetClass, targetMethod, firstMatch, 2)
                ?: return null
            if (arrayType.sort != Type.ARRAY) {
                return null
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val psiArrayType = arrayType.toPsiType(elementFactory) as PsiArrayType
            return listOf(
                MethodSignature(
                    listOf(
                        ParameterGroup(
                            listOf(
                                Parameter("array", psiArrayType),
                                Parameter("index", PsiType.INT),
                                Parameter("value", psiArrayType.componentType)
                            )
                        )
                    ),
                    PsiType.VOID
                )
            )
        }
    }

    private object Constructor : RedirectType {
        override fun isInsnAllowed(node: AbstractInsnNode): Boolean {
            return NewInsnInjectionPoint.findInitCall(node as TypeInsnNode) != null
        }

        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature> {
            val firstMatch = insns.first() as TypeInsnNode
            val isValid = insns.all {
                val insn = it as? TypeInsnNode ?: return@all false
                insn.opcode == Opcodes.NEW && insn.desc == firstMatch.desc
            }
            if (!isValid) {
                return emptyList()
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
            val constructedType = Type.getObjectType(firstMatch.desc).toPsiType(elementFactory)

            return Method.expectedMethodSignature(
                annotation,
                targetClass,
                targetMethod,
                insns.mapNotNull {
                    NewInsnInjectionPoint.findInitCall(it as TypeInsnNode)
                }
            ).map { (paramGroups, _) ->
                // drop the instance parameter, return the constructed type
                MethodSignature(listOf(ParameterGroup(paramGroups[0].parameters.drop(1))), constructedType)
            }
        }
    }

    private object InstanceOf : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insns: List<AbstractInsnNode>
        ): List<MethodSignature> {
            val firstMatch = insns.first()
            val isValid = insns.all { it.opcode == firstMatch.opcode }
            if (!isValid) return emptyList()

            val psiManager = PsiManager.getInstance(annotation.project)
            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
            val objectType = PsiType.getJavaLangObject(psiManager, annotation.resolveScope)
            val classType = elementFactory.createTypeFromText("java.lang.Class<?>", annotation)
            val parameters = ParameterGroup(
                listOf(
                    Parameter("instance", objectType),
                    Parameter("type", classType)
                )
            )
            return listOf(
                MethodSignature(listOf(parameters), PsiType.BOOLEAN),
                MethodSignature(listOf(parameters), classType)
            )
        }
    }
}
