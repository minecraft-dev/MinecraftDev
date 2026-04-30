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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
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

class ModifyArgsHandler : InjectorAnnotationHandler() {
    override fun isInsnAllowed(insn: AbstractInsnNode, decorations: Map<String, Any?>): Boolean {
        return insn is MethodInsnNode
    }

    override val allowedInsnDescription = "method invocations"

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        enclosingSelector: MixinSelector?,
    ): List<MethodSignature> {
        val argsType = JavaPsiFacade.getElementFactory(annotation.project)
            .createTypeByFQClassName(ARGS, annotation.resolveScope)
        return listOf(
            MethodSignature(
                listOf(
                    ParameterGroup(listOf(Parameter("args", argsType))),
                    ParameterGroup(
                        collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                        required = ParameterGroup.RequiredLevel.OPTIONAL,
                        isVarargs = true,
                    ),
                ),
                PsiTypes.voidType(),
            ),
        )
    }

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.MODIFY_ARGS
}
