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

package com.demonwav.mcdev.platform.mixin.inspection.shadow

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.hasNamedLocalVariables
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.siyeh.ig.fixes.RenameFix
import org.objectweb.asm.Opcodes

class ShadowParameterNameInspection : MixinInspection() {
    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.Annotations.SHADOW)) {
                return
            }

            val target = MixinAnnotationHandler.resolveTarget(annotation).firstOrNull() as? MethodTargetMember ?: return

            if (!annotation.hasNamedLocalVariables(target.classAndMethod.clazz.name.replace('/', '.'))) {
                return
            }
            val thisLocals = if (target.classAndMethod.method.hasAccess(Opcodes.ACC_STATIC)) 0 else 1
            val targetLocals = target.classAndMethod.method.localVariables?.sortedBy { it.index }?.drop(thisLocals) ?: return

            val method = annotation.parent?.parent as? PsiMethod ?: return
            for ((param, targetLocal) in method.parameterList.parameters.zip(targetLocals)) {
                val correctName = targetLocal?.name?.toJavaIdentifier() ?: continue
                if (param.name != correctName) {
                    val problemElement = param.nameIdentifier ?: param
                    holder.registerProblem(
                        problemElement,
                        "Parameter name does not match name '$correctName' in target class",
                        RenameFix(correctName, false, false)
                    )
                }
            }
        }
    }
}
