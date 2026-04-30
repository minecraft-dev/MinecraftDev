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
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.ContextAwareMethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.constantValue
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiExpression
import org.objectweb.asm.util.Printer

class InjectorOpcodeInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports missing or invalid usages of opcode"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.Annotations.AT)) {
                return
            }

            val injectionPoint = AtResolver.getInjectionPoint(annotation) ?: return
            val validOpcodes = injectionPoint.validOpcodes
            val opcodeAttribute = annotation.findDeclaredAttributeValue("opcode")
            val opcode = opcodeAttribute?.constantValue as? Int

            if (validOpcodes.isEmpty()) {
                if (opcode != null) {
                    holder.registerProblem(
                        opcodeAttribute,
                        "Opcode is ignored by this injection point",
                        AnnotationAttributeFix(annotation, "opcode" to null)
                    )
                }
            } else {
                if (opcode == null) {
                    holder.registerProblem(
                        annotation.nameReferenceElement ?: annotation,
                        "Missing opcode",
                        *listOfNotNull(makeSpecifyOpcodeFix(holder.project, annotation)).toTypedArray()
                    )
                } else if (opcode !in validOpcodes) {
                    holder.registerProblem(
                        opcodeAttribute,
                        "Invalid opcode for this injection point"
                    )
                }
            }
        }
    }

    private fun makeSpecifyOpcodeFix(project: Project, at: PsiAnnotation): LocalQuickFix? {
        val injector = AtResolver.findInjectorAnnotation(at) ?: return null
        val targets = MixinAnnotationHandler.resolveTarget(injector)
            .filterIsInstance<ContextAwareMethodTargetMember>()

        val possibleOpcodes = mutableSetOf<Int>()

        for (target in targets) {
            val targetMethod = target.classAndMethod
            val instructions = AtResolver(at, targetMethod.clazz, targetMethod.method, target.selector)
                .resolveInstructions()
            for (insn in instructions) {
                possibleOpcodes += insn.originalInsn.opcode
            }
        }

        val opcode = possibleOpcodes.singleOrNull()?.takeIf { it != -1 } ?: return null
        val opcodeName = Printer.OPCODES[opcode]
        val opcodeValue = JavaPsiFacade.getElementFactory(project).createExpressionFromText(
            "org.objectweb.asm.Opcodes.$opcodeName",
            at
        )
        return AddOpcodeFix(at, opcodeName, opcodeValue)
    }

    private class AddOpcodeFix(at: PsiAnnotation, private val opcodeName: String, opcodeValue: PsiExpression) : AnnotationAttributeFix(
        at,
        "opcode" to opcodeValue
    ) {
        override fun getFamilyName() = "Add opcode = Opcodes.$opcodeName"
        override fun getText() = "Add opcode = Opcodes.$opcodeName"
    }
}
