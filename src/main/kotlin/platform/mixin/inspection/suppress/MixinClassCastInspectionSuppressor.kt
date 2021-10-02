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

import com.demonwav.mcdev.platform.mixin.action.FindMixinsAction
import com.demonwav.mcdev.platform.mixin.util.findStubClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.codeInspection.dataFlow.types.DfReferenceType
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDisjunctionType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiInstanceOfExpression
import com.intellij.psi.PsiIntersectionType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.util.InheritanceUtil
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

    private fun isAssignable(left: PsiType, right: PsiType): Boolean {
        return when {
            left is PsiIntersectionType -> left.conjuncts.all { isAssignable(it, right) }
            right is PsiIntersectionType -> right.conjuncts.any { isAssignable(left, it) }
            left is PsiDisjunctionType -> left.disjunctions.any { isAssignable(it, right) }
            right is PsiDisjunctionType -> isAssignable(left, right.leastUpperBound)
            left is PsiArrayType -> right is PsiArrayType && isAssignable(left.componentType, right.componentType)
            else -> {
                if (left !is PsiClassType || right !is PsiClassType) {
                    return false
                }
                val leftClass = left.resolve() ?: return false
                val rightClass = right.resolve() ?: return false
                if (rightClass.isMixin) {
                    val isMixinAssignable = rightClass.mixinTargets.any {
                        val stubClass = it.findStubClass(rightClass.project) ?: return@any false
                        isClassAssignable(leftClass, stubClass)
                    }
                    if (isMixinAssignable) {
                        return true
                    }
                }
                val mixins = FindMixinsAction.findMixins(rightClass, rightClass.project) ?: return false
                return mixins.any { isClassAssignable(leftClass, it) }
            }
        }
    }

    private fun isClassAssignable(leftClass: PsiClass, rightClass: PsiClass): Boolean {
        var result = false
        InheritanceUtil.processSupers(rightClass, true) {
            if (it == leftClass) {
                result = true
                false
            } else {
                true
            }
        }
        return result
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
