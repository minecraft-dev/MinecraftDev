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

import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.TargetInsn
import com.demonwav.mcdev.platform.mixin.inspection.injector.BasicSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.ExpectedSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.SignatureSuggestion
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.ARGS
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiTypes
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class ModifyArgsHandler : InsnInjectorAnnotationHandler() {
    override fun isInsnAllowed(insn: AbstractInsnNode, decorations: Map<String, Any?>): Boolean {
        return insn is MethodInsnNode
    }

    override val allowedInsnDescription = "method invocations"

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        targetInsn: TargetInsn,
    ): ExpectedSignatures<BasicSignatures> {
        val argsType = JavaPsiFacade.getElementFactory(annotation.project)
            .createTypeByFQClassName(ARGS, annotation.resolveScope)
        val shortParams = listOf(Parameter("args", argsType))
        return ExpectedSignatures.Valid(
            BasicSignatures(
                MethodSignature(
                    shortParams,
                    PsiTypes.voidType(),
                    allowCoerceRequired = false,
                ),
                MethodSignature(
                    shortParams + collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                    PsiTypes.voidType(),
                    allowCoerceRequired = false,
                ),
            )
        )
    }

    override fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>
    ): SuggestedSignature {
        val argsType = JavaPsiFacade.getElementFactory(annotation.project)
            .createTypeByFQClassName(ARGS, annotation.resolveScope)
        return SuggestedSignature(
            listOf(SignatureSuggestion.Param("args", argsType)),
            PsiTypes.voidType(),
        )
    }

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.MODIFY_ARGS
}
