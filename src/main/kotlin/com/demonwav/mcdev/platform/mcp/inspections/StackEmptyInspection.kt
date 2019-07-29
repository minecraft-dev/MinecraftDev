/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.inspections

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.srg.SrgMemberReference
import com.demonwav.mcdev.util.MemberReference
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
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class StackEmptyInspection : BaseInspection() {
    companion object {
        const val STACK_FQ_NAME = "net.minecraft.item.ItemStack"
        val STACK_JVM_NAME = STACK_FQ_NAME.replace('.', '/')
        val EMPTY_SRG = SrgMemberReference.parse("$STACK_JVM_NAME/field_190927_a")
        val IS_EMPTY_SRG = SrgMemberReference.parse("$STACK_JVM_NAME/func_190926_b")
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

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        return object : InspectionGadgetsFix() {
            override fun getFamilyName() = "Replace with .isEmpty()"

            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                val compareExpression = infos[0] as PsiExpression
                val binaryExpression = infos[2] as PsiBinaryExpression
                val elementFactory = JavaPsiFacade.getElementFactory(project)

                val mappedIsEmpty = compareExpression.findModule()?.getMappedMethod(IS_EMPTY_SRG)?.name ?: "isEmpty"

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
                    if (isExpressionStack(leftExpression) && isExpressionStack(rightExpression)) {
                        val leftEmpty = isExpressionEmptyConstant(leftExpression)
                        val rightEmpty = isExpressionEmptyConstant(rightExpression)

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

            private fun isExpressionStack(expression: PsiExpression?): Boolean {
                return (expression?.type as? PsiClassType)?.resolve()?.fullQualifiedName == STACK_FQ_NAME
            }

            private fun isExpressionEmptyConstant(expression: PsiExpression?): Boolean {
                val reference = expression as? PsiReferenceExpression ?: return false
                val field = reference.resolve() as? PsiField ?: return false
                val mappedEmpty = field.findModule()?.getMappedField(EMPTY_SRG)?.name ?: "EMPTY"
                return field.name == mappedEmpty && field.containingClass?.fullQualifiedName == STACK_FQ_NAME
            }
        }
    }

    private fun Module.getMappedMethod(reference: MemberReference): MemberReference? {
        val srgManager = MinecraftFacet.getInstance(this, McpModuleType)?.srgManager
        return srgManager?.srgMapNow?.mapToMcpMethod(reference)
    }

    private fun Module.getMappedField(reference: MemberReference): MemberReference? {
        val srgManager = MinecraftFacet.getInstance(this, McpModuleType)?.srgManager
        return srgManager?.srgMapNow?.mapToMcpField(reference)
    }
}
