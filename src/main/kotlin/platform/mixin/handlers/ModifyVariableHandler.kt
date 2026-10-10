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

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.TargetInsn
import com.demonwav.mcdev.platform.mixin.inspection.injector.ExpectedSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.ModifierSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.collectSignatures
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.SequencedMap
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.findModule
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyVariableHandler : InsnInjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        targetInsn: TargetInsn,
    ): ExpectedSignatures<ModifierSignatures> {
        val module = annotation.findModule() ?: return ExpectedSignatures.Unknown
        val targetParams = collectTargetMethodParameters(annotation.project, targetClass, targetMethod)

        val method = annotation.findContainingMethod() ?: return ExpectedSignatures.Unknown
        val localType = method.parameterList.getParameter(0)?.type
        val info = LocalInfo.fromAnnotation(localType, annotation)

        val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
        val result = linkedMapOf<Type, Parameter>()
        val matchedLocals = info.matchLocals(
            module, targetClass, targetMethod, targetInsn.insn,
            CollectVisitor.Mode.SUGGESTION, matchType = false
        ).orEmpty()
        for (local in matchedLocals) {
            val type = Type.getType(local.desc ?: continue)
            result.computeIfAbsent(type) {
                sanitizedParameter(type.toPsiType(elementFactory), local.name, local.isNamed)
            }
        }

        return ExpectedSignatures.Valid(
            ModifierSignatures(
                SequencedMap(result),
                trailingParams = targetParams,
            )
        )
    }

    override fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>
    ): SuggestedSignature? {
        return SuggestedSignature.modifier(
            annotation,
            expectedMethodSignatures(annotation, targets).collectSignatures<ModifierSignatures>() ?: return null,
        )
    }

    override val isShiftAlwaysDiscouraged = false

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.MODIFY_VARIABLE
}
