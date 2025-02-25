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
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_VARIABLE
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ModifyVariableArgsOnlyInspection : MixinInspection() {
    override fun getStaticDescription() =
        "Checks that @ModifyVariable has argsOnly if it targets arguments, which improves performance of the mixin"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                val modifyVariable = method.findAnnotation(MODIFY_VARIABLE) ?: return
                val wantedType = method.parameterList.getParameter(0)?.type ?: return
                val problemElement = modifyVariable.nameReferenceElement ?: return

                val targets = MixinAnnotationHandler.resolveTarget(modifyVariable).ifEmpty { return }
                val methodTargets = targets.asSequence()
                    .filterIsInstance<MethodTargetMember>()
                    .map { it.classAndMethod }

                if (shouldReport(modifyVariable, wantedType, methodTargets)) {
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

    companion object {
        fun shouldReport(
            annotation: PsiAnnotation,
            wantedType: PsiType,
            methodTargets: Sequence<ClassAndMethodNode>,
        ): Boolean {
            if (annotation.findDeclaredAttributeValue("argsOnly")?.constantValue == true) {
                return false
            }

            val ordinal = (annotation.findDeclaredAttributeValue("ordinal")?.constantValue as? Int?)
                ?.takeIf { it != -1 }
            val index = (annotation.findDeclaredAttributeValue("index")?.constantValue as? Int?)
                ?.takeIf { it != -1 }
            if (ordinal == null && index == null && annotation.findDeclaredAttributeValue("name") != null) {
                return false
            }

            val wantedDesc = wantedType.descriptor

            for ((targetClass, targetMethod) in methodTargets) {
                val argTypes = mutableListOf<String?>()
                if (!targetMethod.hasAccess(Opcodes.ACC_STATIC)) {
                    argTypes += "L${targetClass.name};"
                }
                for (arg in Type.getArgumentTypes(targetMethod.desc)) {
                    argTypes += arg.descriptor
                    if (arg.size == 2) {
                        argTypes += null
                    }
                }

                if (ordinal != null) {
                    if (argTypes.asSequence().filter { it == wantedDesc }.count() <= ordinal) {
                        return false
                    }
                } else if (index != null) {
                    if (argTypes.size <= index) {
                        return false
                    }
                } else {
                    if (argTypes.asSequence().filter { it == wantedDesc }.count() != 1) {
                        return false
                    }
                }
            }

            return true
        }
    }
}
