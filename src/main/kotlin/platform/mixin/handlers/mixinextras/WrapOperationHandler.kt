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
import com.demonwav.mcdev.platform.mixin.util.mixinExtrasOperationType
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class WrapOperationHandler : MixinExtrasInjectorAnnotationHandler() {
    override val supportedInstructionTypes = listOf(
        InstructionType.METHOD_CALL, InstructionType.FIELD_GET, InstructionType.FIELD_SET, InstructionType.INSTANCEOF,
        InstructionType.INSTANTIATION, InstructionType.SIMPLE_OPERATION
    )

    override fun getAtKey(annotation: PsiAnnotation): String {
        return if (annotation.hasAttribute("constant")) "constant" else "at"
    }

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        target: TargetInsn
    ): Pair<ParameterGroup, PsiType>? {
        val params = getParameterTypes(target, targetClass, annotation) ?: return null
        val returnType = getReturnType(target, annotation) ?: return null
        val operationType = mixinExtrasOperationType(annotation, returnType) ?: return null
        return ParameterGroup(
            params + Parameter("original", operationType)
        ) to returnType
    }

    override fun intLikeTypePositions(target: TargetInsn) = buildList {
        if (
            target.getDecoration<Type>(ExpressionDecorations.SIMPLE_OPERATION_RETURN_TYPE)
            == ExpressionASMUtils.INTLIKE_TYPE
        ) {
            add(MethodSignature.TypePosition.Return)
        }
        target.getDecoration<Array<Type>>(ExpressionDecorations.SIMPLE_OPERATION_ARGS)?.forEachIndexed { i, it ->
            if (it == ExpressionASMUtils.INTLIKE_TYPE) {
                add(MethodSignature.TypePosition.Param(i))
            }
        }
    }

    private fun getParameterTypes(
        target: TargetInsn,
        targetClass: ClassNode,
        annotation: PsiAnnotation
    ): List<Parameter>? {
        getPsiParameters(target.insn, targetClass, annotation)?.let { return it }
        val args = target.getDecoration<Array<Type>>(ExpressionDecorations.SIMPLE_OPERATION_ARGS) ?: return null
        return args.toList().toParameters(
            annotation,
            target.getDecoration(ExpressionDecorations.SIMPLE_OPERATION_PARAM_NAMES)
        )
    }

    private fun getReturnType(
        target: TargetInsn,
        annotation: PsiAnnotation
    ): PsiType? {
        getPsiReturnType(target.insn, annotation)?.let { return it }
        val type = target.getDecoration<Type>(ExpressionDecorations.SIMPLE_OPERATION_RETURN_TYPE) ?: return null
        return type.toPsiType(JavaPsiFacade.getElementFactory(annotation.project))
    }

    override val defaultOrder = MixinConstants.InjectorOrder.WRAP_OPERATION

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.WRAP_OPERATION
}
