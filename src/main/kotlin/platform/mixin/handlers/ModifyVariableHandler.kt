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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AbstractLoadInjectionPoint
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.findModule
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyVariableHandler : InjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ): List<MethodSignature>? {
        val module = annotation.findModule() ?: return null

        val at = annotation.findAttributeValue("at") as? PsiAnnotation
        val atCode = at?.findAttributeValue("value")?.constantStringValue
        val isLoadStore = atCode != null && InjectionPoint.byAtCode(atCode) is AbstractLoadInjectionPoint
        val mode = if (isLoadStore) CollectVisitor.Mode.COMPLETION else CollectVisitor.Mode.MATCH_ALL
        val targets = resolveInstructions(annotation, targetClass, targetMethod, mode)

        val targetParamsGroup = ParameterGroup(
            collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
            required = ParameterGroup.RequiredLevel.OPTIONAL,
            isVarargs = true,
        )

        val method = annotation.findContainingMethod() ?: return null
        val localType = method.parameterList.getParameter(0)?.type
        val info = LocalInfo.fromAnnotation(localType, annotation)

        val possibleTypes = mutableSetOf<String>()
        for (insn in targets) {
            val locals = info.getLocals(module, targetClass, targetMethod, insn.insn) ?: continue
            val matchedLocals = info.matchLocals(locals, CollectVisitor.Mode.COMPLETION, matchType = false)
            for (local in matchedLocals) {
                possibleTypes += local.desc!!
            }
        }

        val result = mutableListOf<MethodSignature>()

        val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
        for (type in possibleTypes) {
            val psiType = Type.getType(type).toPsiType(elementFactory)
            result += MethodSignature(
                listOf(
                    ParameterGroup(listOf(sanitizedParameter(psiType, "value"))),
                    targetParamsGroup,
                ),
                psiType,
            )
        }

        return result
    }
}
