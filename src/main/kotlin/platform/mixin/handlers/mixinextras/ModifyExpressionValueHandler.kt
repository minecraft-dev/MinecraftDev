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

package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.injector.GeneralSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.collectSignatures
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.SequencedSet
import com.demonwav.mcdev.util.emptySequencedSet
import com.demonwav.mcdev.util.sequencedSetOf
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyExpressionValueHandler : MixinExtrasInjectorAnnotationHandler() {
    override val supportedInstructionTypes = listOf(
        InstructionType.METHOD_CALL, InstructionType.FIELD_GET, InstructionType.INSTANTIATION, InstructionType.CONSTANT,
        InstructionType.SIMPLE_EXPRESSION, InstructionType.STRING_CONCAT_EXPRESSION
    )

    override fun isInsnAllowed(insn: AbstractInsnNode, decorations: Map<String, Any?>): Boolean {
        if (!super.isInsnAllowed(insn, decorations)) {
            return false
        }

        if (decorations[ExpressionDecorations.SIMPLE_EXPRESSION_TYPE] != null) {
            return true
        }

        val returnType = getInsnReturnType(insn) ?: return false
        return returnType != Type.VOID_TYPE
    }

    override val allowedInsnDescription = "instructions that return a value"

    override fun expectedMethodSignatureImpl(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        target: TargetInsn
    ): GeneralSignatures? {
        val psiType = getReturnType(target, annotation) ?: return null
        return GeneralSignatures(
            listOf(Parameter("original", psiType)),
            psiType,
            trailingParams = collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
            intLikePositions = intLikeTypePositions(target),
        )
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

    private fun intLikeTypePositions(target: TargetInsn): SequencedSet<MethodSignature.TypePosition> {
        val expressionType = target.getDecoration<Type>(ExpressionDecorations.SIMPLE_EXPRESSION_TYPE)
        if (expressionType == ExpressionASMUtils.INTLIKE_TYPE) {
            return sequencedSetOf(MethodSignature.TypePosition.Return, MethodSignature.TypePosition.Param(0))
        }
        return emptySequencedSet()
    }

    private fun getReturnType(
        target: TargetInsn,
        annotation: PsiAnnotation
    ): PsiType? {
        if (target.hasDecoration(ExpressionDecorations.IS_STRING_CONCAT_EXPRESSION)) {
            return PsiType.getJavaLangString(annotation.manager, annotation.resolveScope)
        }
        val psiReturnType = getPsiReturnType(target.insn, annotation)
        val rawReturnType = getInsnReturnType(target.insn)
        val exprType = target.getDecoration<Type>(ExpressionDecorations.SIMPLE_EXPRESSION_TYPE)
        if (exprType != null && rawReturnType != exprType) {
            // The expression knows more than the standard logic does.
            return exprType.toPsiType(JavaPsiFacade.getElementFactory(annotation.project))
        }
        return psiReturnType
    }

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.MODIFY_EXPRESSION_VALUE
}
