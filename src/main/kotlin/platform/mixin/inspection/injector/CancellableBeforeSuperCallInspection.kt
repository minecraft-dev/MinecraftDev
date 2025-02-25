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
import com.demonwav.mcdev.platform.mixin.inspection.MixinCancellableInspection
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.findDelegateConstructorCall
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.util.constantValue
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor

class CancellableBeforeSuperCallInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when cancellable @Injects are used before a superconstructor call"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (annotation.qualifiedName != MixinConstants.Annotations.INJECT) {
                return
            }
            val cancellableAttr =
                annotation.parameterList.attributes.firstOrNull { it.attributeName == "cancellable" } ?: return
            if (cancellableAttr.value?.constantValue != true) {
                return
            }
            if (doesInjectBeforeSuperConstructorCall(annotation)) {
                holder.registerProblem(
                    cancellableAttr,
                    "@Inject is cancellable before a superconstructor call",
                    MixinCancellableInspection.RemoveInjectCancellableFix(annotation)
                )
            }
        }
    }

    companion object {
        fun doesInjectBeforeSuperConstructorCall(annotation: PsiAnnotation): Boolean {
            val handler = MixinAnnotationHandler.forMixinAnnotation(MixinConstants.Annotations.INJECT)!!
                as InjectorAnnotationHandler

            for (target in MixinAnnotationHandler.resolveTarget(annotation)) {
                if (target !is MethodTargetMember) {
                    continue
                }
                if (!target.classAndMethod.method.isConstructor) {
                    continue
                }
                val methodInsns = target.classAndMethod.method.instructions ?: continue
                val delegateCtorCall = target.classAndMethod.method.findDelegateConstructorCall() ?: continue
                val instructions =
                    handler.resolveInstructions(annotation, target.classAndMethod.clazz, target.classAndMethod.method)
                if (instructions.any { methodInsns.indexOf(it.insn) <= methodInsns.indexOf(delegateCtorCall) }) {
                    return true
                }
            }

            return false
        }
    }
}
