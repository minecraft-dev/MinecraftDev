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

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.inspection.injector.ExpectedSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.OperationWrapperSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.collectSignatures
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.platform.mixin.util.getGenericReturnType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class WrapMethodHandler : InjectorAnnotationHandler() {
    override fun expectedMethodSignatures(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>,
        mode: CollectVisitor.Mode,
    ): List<ExpectedSignatures<OperationWrapperSignatures>> {
        return targets.map { (targetClass, targetMethod) ->
            val returnType = targetMethod.getGenericReturnType(targetClass, annotation.project)

            ExpectedSignatures.Valid(
                OperationWrapperSignatures(
                    annotation,
                    collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                    returnType,
                ) ?: return@map ExpectedSignatures.Invalid
            )
        }
    }

    override fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>
    ): SuggestedSignature? {
        return SuggestedSignature.operationWrapper(
            annotation,
            expectedMethodSignatures(annotation, targets).collectSignatures<OperationWrapperSignatures>() ?: return null
        )
    }

    override fun isUnresolved(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): InsnResolutionInfo.Failure? {
        // If we've got a target method that's good enough
        return null
    }

    override fun resolveForNavigation(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<PsiElement> {
        val project = annotation.project
        return targetMethod.findSourceElement(
            targetClass,
            project,
            GlobalSearchScope.allScope(project),
            canDecompile = true
        )?.let(::listOf).orEmpty()
    }

    override fun canAlwaysBeStatic(method: PsiMethod) = false
}
