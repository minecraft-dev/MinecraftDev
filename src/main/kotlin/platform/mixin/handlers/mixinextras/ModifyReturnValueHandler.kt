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
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.collectSignatures
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.getGenericReturnType
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.PsiAnnotation
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyReturnValueHandler : MixinExtrasInjectorAnnotationHandler() {
    override val supportedInstructionTypes = listOf(InstructionType.RETURN)

    override val allowedInsnDescription = "return instructions"

    override fun expectedMethodSignatureImpl(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        target: TargetInsn
    ): GeneralSignatures {
        val returnType = targetMethod.getGenericReturnType(targetClass, annotation.project)
        return GeneralSignatures(
            listOf(Parameter("original", returnType)),
            returnType,
            trailingParams = collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
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

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.MODIFY_RETURN_VALUE
}
