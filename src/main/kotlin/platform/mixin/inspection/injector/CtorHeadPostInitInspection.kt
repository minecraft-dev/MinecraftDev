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

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CtorHeadInjectionPoint
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.enumValueOfOrNull
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import org.objectweb.asm.Opcodes

class CtorHeadPostInitInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when CTOR_HEAD with enforce=POST_INIT doesn't target a field"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.Annotations.AT)) {
                return
            }
            if (annotation.findDeclaredAttributeValue("value")?.constantValue != "CTOR_HEAD") {
                return
            }

            val atArgs = annotation.findDeclaredAttributeValue("args") ?: return
            val enforce = AtResolver.getArgs(annotation)["enforce"]
                ?.let { enumValueOfOrNull<CtorHeadInjectionPoint.EnforceMode>(it) }
                ?: CtorHeadInjectionPoint.EnforceMode.DEFAULT
            if (enforce != CtorHeadInjectionPoint.EnforceMode.POST_INIT) {
                return
            }

            val injectorAnnotation = AtResolver.findInjectorAnnotation(annotation) ?: return
            val targets = MixinAnnotationHandler.resolveTarget(injectorAnnotation)
                .filterIsInstance<MethodTargetMember>()

            if (targets.any {
                it.classAndMethod.method.isConstructor &&
                    AtResolver(annotation, it.classAndMethod.clazz, it.classAndMethod.method)
                        .resolveInstructions()
                        .any { insn -> insn.insn.previous?.opcode != Opcodes.PUTFIELD }
            }
            ) {
                holder.registerProblem(
                    atArgs,
                    "CTOR_HEAD with enforce=POST_INIT doesn't target a field",
                    AnnotationAttributeFix(annotation, "args" to null)
                )
            }
        }
    }
}
