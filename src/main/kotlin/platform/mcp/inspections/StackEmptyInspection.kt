/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.inspections

import com.demonwav.mcdev.platform.mcp.mappings.getMappedClass
import com.demonwav.mcdev.platform.mcp.mappings.getMappedField
import com.demonwav.mcdev.platform.mcp.mappings.getMappedMethod
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.createSmartPointer
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class StackEmptyInspection : BaseInspection() {
    companion object {
        const val STACK_FQ_NAME = "net.minecraft.world.item.ItemStack"
    }

    @Nls
    override fun getDisplayName() = "ItemStack comparison through ItemStack.EMPTY"

    override fun buildErrorString(vararg infos: Any): String {
        val compareExpression = infos[0] as PsiExpression
        return "\"${compareExpression.text}\" compared with ItemStack.EMPTY"
    }

    override fun getStaticDescription() =
        "Comparing an ItemStack to ItemStack.EMPTY to query stack emptiness can cause unwanted issues." +
            "When a stack in an inventory is shrunk, the instance is not replaced with ItemStack.EMPTY, but" +
            " the stack should still be considered empty. Instead, isEmpty() should be called."

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix {
        val compareExpressionPointer = (infos[0] as PsiExpression).createSmartPointer()
        val binaryExpressionPointer = (infos[2] as PsiBinaryExpression).createSmartPointer()
        return object : InspectionGadgetsFix() {
            override fun getFamilyName() = "Replace with .isEmpty()"

            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                val elementFactory = JavaPsiFacade.getElementFactory(project)

                val compareExpression = compareExpressionPointer.element ?: return
                val binaryExpression = binaryExpressionPointer.element ?: return

                val mappedIsEmpty = compareExpression.findModule()?.getMappedMethod(STACK_FQ_NAME, "isEmpty", "()Z")

                var expressionText = "${compareExpression.text}.$mappedIsEmpty()"

                // If we were checking for != ItemStack.EMPTY, use !stack.isEmpty()
                if (binaryExpression.operationSign.tokenType == JavaTokenType.NE) {
                    expressionText = "!$expressionText"
                }

                val replacedExpression =
                    elementFactory.createExpressionFromText(expressionText, binaryExpression.context)
                binaryExpression.replace(replacedExpression)
            }
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitBinaryExpression(expression: PsiBinaryExpression) {
                val operationType = expression.operationSign.tokenType

                if (operationType == JavaTokenType.EQEQ || operationType == JavaTokenType.NE) {
                    val leftExpression = expression.lOperand
                    val rightExpression = expression.rOperand

                    // Check if both operands evaluate to an ItemStack
                    val module = expression.findModule() ?: return
                    if (isExpressionStack(module, leftExpression) && isExpressionStack(module, rightExpression)) {
                        val leftEmpty = isExpressionEmptyConstant(module, leftExpression)
                        val rightEmpty = isExpressionEmptyConstant(module, rightExpression)

                        // Check that only one of the references are ItemStack.EMPTY
                        if (leftEmpty xor rightEmpty) {
                            // The other operand will be the stack
                            val compareExpression = if (leftEmpty) rightExpression else leftExpression
                            val emptyReference = if (leftEmpty) leftExpression else rightExpression

                            registerError(expression, compareExpression, emptyReference, expression)
                        }
                    }
                }
            }

            private fun isExpressionStack(module: Module, expression: PsiExpression?): Boolean {
                val fqn = (expression?.type as? PsiClassType)?.resolve()?.fullQualifiedName
                return fqn == module.getMappedClass(STACK_FQ_NAME)
            }

            private fun isExpressionEmptyConstant(module: Module, expression: PsiExpression?): Boolean {
                val reference = expression as? PsiReferenceExpression ?: return false
                val field = reference.resolve() as? PsiField ?: return false
                val mappedEmpty = module.getMappedField(STACK_FQ_NAME, "EMPTY")
                return field.name == mappedEmpty &&
                    field.containingClass?.fullQualifiedName == module.getMappedClass(STACK_FQ_NAME)
            }
        }
    }
}
