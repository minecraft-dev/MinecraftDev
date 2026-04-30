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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.ContextAwareMethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation

class DisallowedTargetInsnInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports disallowed injector targets"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.Annotations.AT)) {
                return
            }

            val injectorAnnotation = AtResolver.findInjectorAnnotation(annotation, skipThroughSlice = false) ?: return
            val injector = MixinAnnotationHandler.forMixinAnnotation(injectorAnnotation, annotation.project)
                as? InjectorAnnotationHandler ?: return
            val containingClass = injectorAnnotation.findContainingClass() ?: return
            val hasInvalidInstructions = containingClass.mixinTargets.any { targetClass ->
                injector.resolveTarget(injectorAnnotation, targetClass).any { targetMember ->
                    if (targetMember !is MethodTargetMember) {
                        return@any false
                    }

                    val targetMethod = targetMember.classAndMethod
                    AtResolver(
                        annotation,
                        targetMethod.clazz,
                        targetMethod.method,
                        (targetMember as? ContextAwareMethodTargetMember)?.selector
                    ).resolveInstructions().any { !injector.isInsnAllowed(it.insn, it.decorations) }
                }
            }

            if (hasInvalidInstructions) {
                holder.registerProblem(
                    annotation,
                    "This injector can only target ${injector.allowedInsnDescription}"
                )
            }
        }
    }
}
