/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.NewInsnInjectionPoint
import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.TargetInsn
import com.demonwav.mcdev.platform.mixin.inspection.injector.ExpectedSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.GeneralSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.collectSignatures
import com.demonwav.mcdev.platform.mixin.util.AsmDfaUtil
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.TypeKind
import com.demonwav.mcdev.platform.mixin.util.getGenericSignature
import com.demonwav.mcdev.platform.mixin.util.getGenericType
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.sequencedMapOf
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

class RedirectInjectorHandler : InsnInjectorAnnotationHandler() {
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

    override fun isInsnAllowed(insn: AbstractInsnNode, decorations: Map<String, Any?>): Boolean {
        return getRedirectType(insn)?.isInsnAllowed(insn) ?: false
    }

    override val allowedInsnDescription = "redirect targets (see docs)"

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        targetInsn: TargetInsn,
    ): ExpectedSignatures<GeneralSignatures> {
        val extraParams = collectTargetMethodParameters(annotation.project, targetClass, targetMethod)
        val redirectType = getRedirectType(targetInsn.insn) ?: return ExpectedSignatures.Invalid
        return redirectType.expectedMethodSignature(
            annotation,
            targetClass,
            targetMethod,
            targetInsn.insn,
            extraParams,
        )?.let(ExpectedSignatures<*>::Valid) ?: ExpectedSignatures.Invalid
    }

    override fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>
    ): SuggestedSignature? {
        return SuggestedSignature.general(
            annotation,
            expectedMethodSignatures(annotation, targets).collectSignatures<GeneralSignatures>() ?: return null
        )
    }

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.REDIRECT

    private interface RedirectType {
        val allowCoerce: Boolean get() = true

        fun isInsnAllowed(node: AbstractInsnNode) = true

        fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures?
    }

    private abstract class FieldAccess : RedirectType {
        protected fun determineFieldType(
            firstMatch: FieldInsnNode,
            annotation: PsiAnnotation,
        ): Pair<PsiElementFactory, PsiType> {
            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val sourceClassAndField = (
                MemberReference(firstMatch.name, firstMatch.desc, firstMatch.owner.replace('/', '.'))
                    .resolveAsm(annotation.project) as? FieldTargetMember
                )?.classAndField
            val fieldType = sourceClassAndField?.field?.getGenericType(sourceClassAndField.clazz, annotation.project)
                ?: Type.getType(firstMatch.desc).toPsiType(elementFactory)

            return elementFactory to fieldType
        }
    }

    private object FieldGet : FieldAccess() {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures {
            insn as FieldInsnNode

            val (elementFactory, fieldType) = determineFieldType(insn, annotation)
            val parameters = mutableListOf<Parameter>()

            if (insn.opcode == Opcodes.GETFIELD) {
                parameters += Parameter("instance", Type.getObjectType(insn.owner).toPsiType(elementFactory))
            }

            return GeneralSignatures(
                parameters,
                fieldType,
                trailingParams,
            )
        }
    }

    private object FieldSet : FieldAccess() {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures {
            insn as FieldInsnNode

            val (elementFactory, fieldType) = determineFieldType(insn, annotation)
            val parameters = mutableListOf<Parameter>()

            if (insn.opcode == Opcodes.PUTFIELD) {
                parameters += Parameter("instance", Type.getObjectType(insn.owner).toPsiType(elementFactory))
            }
            parameters += Parameter("value", fieldType)

            return GeneralSignatures(
                parameters,
                PsiTypes.voidType(),
                trailingParams,
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
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures {
            insn as MethodInsnNode

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val sourceClassAndMethod = (
                MemberReference(insn.name, insn.desc, insn.owner.replace('/', '.'))
                    .resolveAsm(annotation.project) as? MethodTargetMember
                )?.classAndMethod
            val signature = sourceClassAndMethod?.method?.getGenericSignature(
                sourceClassAndMethod.clazz,
                annotation.project,
            )

            val parameters = mutableListOf<Parameter>()
            if (insn.opcode != Opcodes.INVOKESTATIC) {
                parameters += Parameter("instance", Type.getObjectType(insn.owner).toPsiType(elementFactory))
            }

            val sortedLocals = sourceClassAndMethod?.method?.localVariables?.sortedBy { it.index }
            if (signature != null) {
                signature.second
                    .asSequence()
                    .withIndex()
                    .mapTo(parameters) { (index, type) ->
                        val i = if (insn.opcode == Opcodes.INVOKESTATIC) index else index + 1
                        val name = sortedLocals?.getOrNull(i)?.name?.toJavaIdentifier()
                        sanitizedParameter(type, name, name != null)
                    }
            } else {
                Type.getArgumentTypes(insn.desc).withIndex().mapTo(parameters) { (index, type) ->
                    val i = if (insn.opcode == Opcodes.INVOKESTATIC) index else index + 1
                    val name = sortedLocals?.getOrNull(i)?.name?.toJavaIdentifier()
                    sanitizedParameter(type.toPsiType(elementFactory), name, name != null)
                }
            }

            val returnType = signature?.first ?: Type.getReturnType(insn.desc).toPsiType(elementFactory)
            return GeneralSignatures(
                parameters,
                returnType,
                trailingParams,
            )
        }
    }

    private object ArrayLength : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures? {
            val arrayType = AsmDfaUtil.getStackType(annotation.project, targetClass, targetMethod, insn, 0)
                ?: return null
            if (arrayType.sort != Type.ARRAY) {
                return null
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            return GeneralSignatures(
                listOf(Parameter("array", arrayType.toPsiType(elementFactory))),
                PsiTypes.intType(),
                trailingParams,
            )
        }
    }

    private object ArrayGet : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures? {
            val arrayType = AsmDfaUtil.getStackType(annotation.project, targetClass, targetMethod, insn, 1)
                ?: return null
            if (arrayType.sort != Type.ARRAY) {
                return null
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val psiArrayType = arrayType.toPsiType(elementFactory) as PsiArrayType
            return GeneralSignatures(
                listOf(
                    Parameter("array", psiArrayType),
                    Parameter("index", PsiTypes.intType()),
                ),
                psiArrayType.componentType,
                trailingParams,
            )
        }
    }

    private object ArraySet : RedirectType {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures? {
            val arrayType = AsmDfaUtil.getStackType(annotation.project, targetClass, targetMethod, insn, 2)
                ?: return null
            if (arrayType.sort != Type.ARRAY) {
                return null
            }

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)

            val psiArrayType = arrayType.toPsiType(elementFactory) as PsiArrayType
            return GeneralSignatures(
                listOf(
                    Parameter("array", psiArrayType),
                    Parameter("index", PsiTypes.intType()),
                    Parameter("value", psiArrayType.componentType),
                ),
                PsiTypes.voidType(),
                trailingParams,
            )
        }
    }

    private object Constructor : RedirectType {
        override fun isInsnAllowed(node: AbstractInsnNode): Boolean {
            return NewInsnInjectionPoint.Util.findInitCall(node as TypeInsnNode) != null
        }

        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures? {
            insn as TypeInsnNode

            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
            val constructedType = Type.getObjectType(insn.desc).toPsiType(elementFactory)

            return Method.expectedMethodSignature(
                annotation,
                targetClass,
                targetMethod,
                NewInsnInjectionPoint.Util.findInitCall(insn) ?: return null,
                trailingParams,
            ).let { sig ->
                // drop the instance parameter, return the constructed type
                sig.copy(
                    params = sig.params.drop(1),
                    returnTypeOptions = sequencedMapOf(TypeKind.OBJECT to constructedType),
                )
            }
        }
    }

    private object InstanceOf : RedirectType {
        override val allowCoerce: Boolean get() = false

        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode,
            insn: AbstractInsnNode,
            trailingParams: List<Parameter>,
        ): GeneralSignatures {
            val psiManager = PsiManager.getInstance(annotation.project)
            val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
            val objectType = PsiType.getJavaLangObject(psiManager, annotation.resolveScope)
            val classType = elementFactory.createTypeFromText("java.lang.Class<?>", annotation)
            val parameters = listOf(
                Parameter("instance", objectType),
                Parameter("type", classType),
            )
            return GeneralSignatures(
                parameters,
                sequencedMapOf(
                    TypeKind.INT_LIKE to PsiTypes.booleanType(),
                    TypeKind.OBJECT to classType,
                ),
                allowCoerce = false,
                trailingParams,
            )
        }
    }
}
