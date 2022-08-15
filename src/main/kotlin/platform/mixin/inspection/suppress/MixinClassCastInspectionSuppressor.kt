/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.suppress

import com.demonwav.mcdev.platform.mixin.action.FindMixinsAction
import com.demonwav.mcdev.platform.mixin.util.isAssignable
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.codeInspection.dataFlow.TypeConstraint
import com.intellij.codeInspection.dataFlow.TypeConstraints
import com.intellij.codeInspection.dataFlow.types.DfReferenceType
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiClassType
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

        // check == and !=
        if (element is PsiBinaryExpression && (
            element.operationSign.tokenType == JavaTokenType.EQEQ ||
                element.operationSign.tokenType == JavaTokenType.NE
            )
        ) {
            val rightType = element.rOperand?.let(this::getTypeConstraint) ?: return false
            val leftType = getTypeConstraint(element.lOperand) ?: return false
            val isTypeWarning = leftType.meet(rightType) == TypeConstraints.BOTTOM
            if (isTypeWarning) {
                val leftWithMixins = addMixinsToTypeConstraint(element.project, leftType)
                val rightWithMixins = addMixinsToTypeConstraint(element.project, rightType)
                if (leftWithMixins == leftType && rightWithMixins == rightType) {
                    return false
                }
                return leftWithMixins.meet(rightWithMixins) != TypeConstraints.BOTTOM
            }
        }

        val castExpression = element.parent as? PsiTypeCastExpression ?: return false
        val castType = castExpression.type ?: return false
        val realType = getRealType(castExpression) ?: return false

        return isAssignable(castType, realType)
    }

    private fun addMixinsToTypeConstraint(project: Project, typeConstraint: TypeConstraint): TypeConstraint {
        val psiType = typeConstraint.getPsiType(project) ?: return typeConstraint
        val targetClass = when (psiType) {
            is PsiArrayType -> (psiType.deepComponentType as? PsiClassType)?.resolve()
            is PsiClassType -> psiType.resolve()
            else -> null
        } ?: return typeConstraint
        val mixins = FindMixinsAction.findMixins(targetClass, project) ?: return typeConstraint
        if (mixins.isEmpty()) return typeConstraint
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val mixinTypes = mixins.map { mixinClass ->
            var type: PsiType = elementFactory.createType(mixinClass)
            if (psiType is PsiArrayType) {
                repeat(psiType.arrayDimensions) {
                    type = type.createArrayType()
                }
            }
            if (typeConstraint.isExact) {
                TypeConstraints.exact(type)
            } else {
                TypeConstraints.instanceOf(type)
            }
        }
        return typeConstraint.join(mixinTypes.reduce(TypeConstraint::join))
    }

    private fun getRealType(expression: PsiExpression): PsiType? {
        return getTypeConstraint(expression)?.getPsiType(expression.project)
    }

    private fun getTypeConstraint(expression: PsiExpression): TypeConstraint? {
        return (CommonDataflow.getDfType(expression) as? DfReferenceType)?.constraint
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    companion object {
        private const val INSPECTION = "ConstantConditions"
    }
}
