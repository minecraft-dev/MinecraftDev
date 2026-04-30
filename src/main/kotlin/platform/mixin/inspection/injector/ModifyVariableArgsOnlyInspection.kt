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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.ContextAwareMethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_VARIABLE
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ModifyVariableArgsOnlyInspection : MixinInspection() {
    override fun getStaticDescription() =
        "Checks that @ModifyVariable has argsOnly if it targets arguments, which improves performance of the mixin"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                val modifyVariable = method.findAnnotation(MODIFY_VARIABLE) ?: return
                val localType = method.parameterList.getParameter(0)?.type ?: return
                val problemElement = modifyVariable.nameReferenceElement ?: return

                val injector =
                    MixinAnnotationHandler.forMixinAnnotation(MODIFY_VARIABLE) as? InjectorAnnotationHandler ?: return
                val localInfo = LocalInfo.fromAnnotation(localType, modifyVariable)

                if (Util.shouldReport(localInfo, injector, modifyVariable)) {
                    val description = "@ModifyVariable may be argsOnly = true"
                    holder.registerProblem(
                        problemElement,
                        description,
                        AnnotationAttributeFix(modifyVariable, "argsOnly" to true),
                    )
                }
            }
        }
    }

    object Util {
        fun shouldReport(
            localInfo: LocalInfo,
            injector: InjectorAnnotationHandler,
            injectorAnnotation: PsiAnnotation,
        ): Boolean {
            if (localInfo.argsOnly) {
                return false
            }

            val localInfo = LocalInfo(
                localInfo.type,
                argsOnly = true,
                localInfo.index,
                localInfo.ordinal,
                localInfo.names,
            )

            val module = injectorAnnotation.findModule() ?: return false

            for (targetMember in MixinAnnotationHandler.resolveTarget(injectorAnnotation)) {
                val targetMethod = (targetMember as? MethodTargetMember)?.classAndMethod ?: continue
                val resolvedInsns = injector.resolveInstructions(
                    injectorAnnotation,
                    targetMethod.clazz,
                    targetMethod.method,
                    (targetMember as? ContextAwareMethodTargetMember)?.selector
                )

                if (resolvedInsns.isEmpty()) {
                    // unresolved injection point, don't report that we can be argsOnly
                    return false
                }

                var argumentsSize = Type.getArgumentsAndReturnSizes(targetMethod.method.desc) shr 2
                if (targetMethod.method.hasAccess(Opcodes.ACC_STATIC)) {
                    argumentsSize--
                }

                for (insn in resolvedInsns) {
                    val matchedLocals = localInfo.matchLocals(
                        module,
                        targetMethod.clazz,
                        targetMethod.method,
                        insn.insn,
                        CollectVisitor.Mode.RESOLUTION
                    )
                    if (matchedLocals.isNullOrEmpty() || matchedLocals.any { it.index >= argumentsSize }) {
                        return false
                    }
                }
            }

            return true
        }
    }
}
