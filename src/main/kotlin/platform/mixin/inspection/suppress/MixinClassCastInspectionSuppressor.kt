/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.suppress

import com.demonwav.mcdev.platform.mixin.util.isAssignable
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.codeInspection.dataFlow.types.DfReferenceType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiInstanceOfExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.util.PsiUtil

/**
 * Looks for `ConstantConditions` warnings on type casts and checks if they are casts to interfaces introduced by mixins
 */
class MixinClassCastInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION) {
            return false
        }

        // check instanceof
        if (element is PsiInstanceOfExpression) {
            val castType = element.checkType?.type ?: return false
            var operand = PsiUtil.skipParenthesizedExprDown(element.operand) ?: return false
            while (operand is PsiTypeCastExpression) {
                operand = PsiUtil.skipParenthesizedExprDown(operand.operand) ?: return false
            }
            val realType = getRealType(operand) ?: return false
            return isAssignable(castType, realType)
        }

        val castExpression = element.parent as? PsiTypeCastExpression ?: return false
        val castType = castExpression.type ?: return false
        val realType = getRealType(castExpression) ?: return false

        return isAssignable(castType, realType)
    }

    private fun getRealType(expression: PsiExpression): PsiType? {
        return (CommonDataflow.getDfType(expression) as? DfReferenceType)?.constraint?.getPsiType(expression.project)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    companion object {
        private const val INSPECTION = "ConstantConditions"
    }
}
