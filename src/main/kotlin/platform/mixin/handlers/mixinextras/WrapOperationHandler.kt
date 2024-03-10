/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.OPERATION
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.llamalad7.mixinextras.utils.Decorations
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
        val operationType = getOperationType(annotation, returnType) ?: return null
        return ParameterGroup(
            params + Parameter("original", operationType)
        ) to returnType
    }

    private fun getParameterTypes(
        target: TargetInsn,
        targetClass: ClassNode,
        annotation: PsiAnnotation
    ): List<Parameter>? {
        getPsiParameters(target.insn, targetClass, annotation)?.let { return it }
        val args = target.getDecoration<Array<Type>>(Decorations.SIMPLE_OPERATION_ARGS) ?: return null
        return args.toList().toParameters(annotation, target.getDecoration(Decorations.SIMPLE_OPERATION_PARAM_NAMES))
    }

    private fun getReturnType(
        target: TargetInsn,
        annotation: PsiAnnotation
    ): PsiType? {
        getPsiReturnType(target.insn, annotation)?.let { return it }
        val type = target.getDecoration<Type>(Decorations.SIMPLE_OPERATION_RETURN_TYPE) ?: return null
        return type.toPsiType(JavaPsiFacade.getElementFactory(annotation.project))
    }

    private fun getOperationType(context: PsiElement, type: PsiType): PsiType? {
        val project = context.project
        val boxedType = if (type is PsiPrimitiveType) {
            type.getBoxedType(context) ?: return null
        } else {
            type
        }

        return JavaPsiFacade.getElementFactory(project)
            .createTypeFromText("$OPERATION<${boxedType.canonicalText}>", context)
    }
}
