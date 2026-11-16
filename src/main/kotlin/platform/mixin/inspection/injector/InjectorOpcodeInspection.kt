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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.constantValue
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.options.OptPane
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiExpression
import org.objectweb.asm.util.Printer

class InjectorOpcodeInspection : MixinInspection() {
    @JvmField
    var reportIgnored = true
    @JvmField
    var reportInvalid = true
    @JvmField
    var reportMissing = true

    override fun getOptionsPane() = OptPane.pane(
        OptPane.checkbox("reportIgnored", MCDevBundle("inspection.missing_or_invalid_opcode.option.ignore")),
        OptPane.checkbox("reportInvalid", MCDevBundle("inspection.missing_or_invalid_opcode.option.invalid")),
        OptPane.checkbox("reportMissing", MCDevBundle("inspection.missing_or_invalid_opcode.option.missing"))
    )

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
                if (opcode != null && reportIgnored) {
                    holder.registerProblem(
                        opcodeAttribute,
                        MCDevBundle("inspection.missing_or_invalid_opcode.ignored"),
                        AnnotationAttributeFix(annotation, "opcode" to null)
                    )
                }
            } else {
                if (opcode == null) {
                    if (reportMissing) {
                        holder.registerProblem(
                            annotation.nameReferenceElement ?: annotation,
                            MCDevBundle("inspection.missing_or_invalid_opcode.missing"),
                            *listOfNotNull(makeSpecifyOpcodeFix(holder.project, annotation)).toTypedArray()
                        )
                    }
                } else if (opcode !in validOpcodes && reportInvalid) {
                    holder.registerProblem(
                        opcodeAttribute,
                        MCDevBundle("inspection.missing_or_invalid_opcode.invalid")
                    )
                }
            }
        }
    }

    private fun makeSpecifyOpcodeFix(project: Project, at: PsiAnnotation): LocalQuickFix? {
        val injector = AtResolver.findInjectorAnnotation(at) ?: return null
        val targetMethods =
            MixinAnnotationHandler.resolveTarget(injector).filterIsInstance<MethodTargetMember>()

        val possibleOpcodes = mutableSetOf<Int>()

        for (method in targetMethods) {
            val instructions = AtResolver(at, method.classAndMethod.clazz, method.classAndMethod.method)
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
        override fun getFamilyName() = MCDevBundle("inspection.missing_or_invalid_opcode.fix", opcodeName)
        override fun getText() = MCDevBundle("inspection.missing_or_invalid_opcode.fix", opcodeName)
    }
}
