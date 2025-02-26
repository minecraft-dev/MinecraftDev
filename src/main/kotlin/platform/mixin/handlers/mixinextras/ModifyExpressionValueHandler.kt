/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.Parameter
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

    override fun extraTargetRestrictions(insn: AbstractInsnNode): Boolean {
        val returnType = getInsnReturnType(insn) ?: return false
        return returnType != Type.VOID_TYPE
    }

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        target: TargetInsn
    ): Pair<ParameterGroup, PsiType>? {
        val psiType = getReturnType(target, annotation) ?: return null
        return ParameterGroup(listOf(Parameter("original", psiType))) to psiType
    }

    override fun intLikeTypePositions(target: TargetInsn): List<MethodSignature.TypePosition> {
        val expressionType = target.getDecoration<Type>(ExpressionDecorations.SIMPLE_EXPRESSION_TYPE)
        if (expressionType == ExpressionASMUtils.INTLIKE_TYPE) {
            return listOf(MethodSignature.TypePosition.Return, MethodSignature.TypePosition.Param(0))
        }
        return emptyList()
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

    override val defaultOrder = MixinConstants.InjectorOrder.MODIFY_EXPRESSION_VALUE

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.MODIFY_EXPRESSION_VALUE
}
