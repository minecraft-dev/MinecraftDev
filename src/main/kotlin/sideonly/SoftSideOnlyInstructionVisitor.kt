/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

import com.intellij.codeInsight.daemon.impl.quickfix.SimplifyBooleanExpressionFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.dataFlow.DataFlowRunner
import com.intellij.codeInspection.dataFlow.DfaInstructionState
import com.intellij.codeInspection.dataFlow.DfaMemoryState
import com.intellij.codeInspection.dataFlow.StandardInstructionVisitor
import com.intellij.codeInspection.dataFlow.instructions.InstanceofInstruction
import com.intellij.codeInspection.dataFlow.instructions.MethodCallInstruction
import com.intellij.codeInspection.dataFlow.instructions.MethodReferenceInstruction
import com.intellij.codeInspection.dataFlow.instructions.PushInstruction
import com.intellij.codeInspection.dataFlow.instructions.TypeCastInstruction
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory
import com.intellij.codeInspection.dataFlow.value.DfaVariableValue
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiInstanceOfExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil

class SoftSideOnlyInstructionVisitor(
    private val body: PsiElement,
    private val factory: DfaValueFactory,
    private val problems: ProblemsHolder
) : StandardInstructionVisitor() {
    private val contextSide = SideOnlyUtil.getContextSide(body, SideHardness.EITHER)
    private val isLogicalClientVars = mutableListOf<DfaVariableValue>()

    override fun visitPush(
        instruction: PushInstruction,
        runner: DataFlowRunner,
        memState: DfaMemoryState
    ): Array<DfaInstructionState> {
        val dfaVar = instruction.value as? DfaVariableValue
            ?: return super.visitPush(instruction, runner, memState)

        val expression = instruction.expression

        if (expression is PsiClassObjectAccessExpression) {
            val targetClass = SideOnlyUtil.getClassInType(expression.operand.type)
                ?: return super.visitPush(instruction, runner, memState)
            val problemElement = expression.operand
            checkAccess(memState, problemElement, targetClass)
            return super.visitPush(instruction, runner, memState)
        }

        val field = dfaVar.psiVariable as? PsiField ?: return super.visitPush(instruction, runner, memState)
        val problemElement = (expression as? PsiReferenceExpression)?.referenceNameElement

        if (field.containingClass?.qualifiedName == "net.minecraft.world.World" &&
            (field.name == "isRemote" || field.name == "isClient")
        ) {
            if (isPhysicalServer(memState)) {
                memState.setVarValue(dfaVar, factory.constFactory.`false`)
                if (problemElement != null) {
                    val fix = createSimplifyBooleanFix(expression, false)
                    problems.registerProblem(
                        problemElement,
                        "Expression is always false",
                        *listOfNotNull(fix).toTypedArray()
                    )
                }
            }
            isLogicalClientVars.add(dfaVar)
        } else {
            if (problemElement != null) {
                checkAccess(memState, problemElement, field)
            }
        }

        return super.visitPush(instruction, runner, memState)
    }

    override fun visitMethodCall(
        instruction: MethodCallInstruction,
        runner: DataFlowRunner,
        memState: DfaMemoryState
    ): Array<DfaInstructionState> {
        val result = super.visitMethodCall(instruction, runner, memState)

        val targetMethod = instruction.targetMethod ?: return result
        val problemElement = when (val callExpression = instruction.callExpression) {
            is PsiMethodCallExpression -> callExpression.methodExpression.referenceNameElement
            is PsiNewExpression -> callExpression.classOrAnonymousClassReference
            else -> return result
        } ?: return result
        checkAccess(memState, problemElement, targetMethod)

        return result
    }

    override fun visitTypeCast(
        instruction: TypeCastInstruction,
        runner: DataFlowRunner,
        memState: DfaMemoryState
    ): Array<DfaInstructionState> {
        val result = super.visitTypeCast(instruction, runner, memState)

        val targetClass = SideOnlyUtil.getClassInType(instruction.castTo) ?: return result
        val problemElement = instruction.expression.castType ?: return result
        checkAccess(memState, problemElement, targetClass)

        return result
    }

    override fun visitInstanceof(
        instruction: InstanceofInstruction,
        runner: DataFlowRunner,
        memState: DfaMemoryState
    ): Array<DfaInstructionState> {
        val result = super.visitInstanceof(instruction, runner, memState)

        val targetType = instruction.castType ?: return result
        val targetClass = SideOnlyUtil.getClassInType(targetType) ?: return result
        val problemElement = (instruction.expression as? PsiInstanceOfExpression)?.checkType ?: return result
        checkAccess(memState, problemElement, targetClass)

        return result
    }

    override fun visitMethodReference(
        instruction: MethodReferenceInstruction,
        runner: DataFlowRunner,
        memState: DfaMemoryState
    ): Array<DfaInstructionState> {
        val result = super.visitMethodReference(instruction, runner, memState)

        val targetMember = instruction.expression.potentiallyApplicableMember ?: return result
        val problemElement = instruction.expression.referenceNameElement ?: return result
        checkAccess(memState, problemElement, targetMember)

        return result
    }

    private fun checkAccess(memState: DfaMemoryState, problemElement: PsiElement, targetElement: PsiElement) {
        val targetSide = SideOnlyUtil.getContextSide(targetElement, SideHardness.SOFT)
        if (targetSide?.side == Side.CLIENT && !isPhysicalClient(memState)) {
            problems.registerProblem(problemElement, SideOnlyUtil.createInspectionMessage(contextSide, targetSide))
        } else if (targetSide?.side == Side.SERVER && !isPhysicalServer(memState)) {
            problems.registerProblem(problemElement, SideOnlyUtil.createInspectionMessage(contextSide, targetSide))
        }
    }

    private fun isPhysicalClient(memState: DfaMemoryState): Boolean {
        return contextSide?.side == Side.CLIENT ||
            isLogicalClientVars.any { memState.getConstantValue(it) == factory.constFactory.`true` }
    }

    private fun isPhysicalServer(memState: DfaMemoryState): Boolean {
        return contextSide?.side == Side.SERVER
    }

    private fun createSimplifyBooleanFix(element: PsiElement, value: Boolean): LocalQuickFixOnPsiElement? {
        val expression = element as? PsiExpression ?: return null
        if (PsiTreeUtil.findChildOfType(expression, PsiAssignmentExpression::class.java) != null) return null
        var wholeExpression = expression
        var parent = expression.parent
        while (parent is PsiExpression) {
            wholeExpression = parent
            parent = wholeExpression.parent
        }
        val fix = SimplifyBooleanExpressionFix(expression, value)
        // simplify intention already active
        if (!fix.isAvailable || SimplifyBooleanExpressionFix.canBeSimplified(wholeExpression)) return null

        return fix
    }
}
