/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection.suppress

import com.demonwav.mcdev.platform.mixin.action.FindMixinsAction
import com.demonwav.mcdev.platform.mixin.util.isAssignable
import com.demonwav.mcdev.util.McdevDfaUtil
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.dataFlow.TypeConstraint
import com.intellij.codeInspection.dataFlow.TypeConstraints
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInstanceOfExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiTypeTestPattern
import com.intellij.psi.util.JavaPsiPatternUtil
import com.intellij.psi.util.PsiUtil

/**
 * Looks for `ConstantConditions`, `ConstantValue` and `DataFlowIssue` warnings on type casts and checks if they are
 * casts to interfaces introduced by mixins
 */
class MixinClassCastInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId !in INSPECTIONS) {
            return false
        }

        // check instanceof
        if (element is PsiInstanceOfExpression) {
            val castType = element.checkType?.type
                ?: (JavaPsiPatternUtil.skipParenthesizedPatternDown(element.pattern) as? PsiTypeTestPattern)
                    ?.checkType?.type
                ?: return false
            var operand = PsiUtil.skipParenthesizedExprDown(element.operand) ?: return false
            while (operand is PsiTypeCastExpression) {
                operand = PsiUtil.skipParenthesizedExprDown(operand.operand) ?: return false
            }
            val realType = McdevDfaUtil.tryGetDataflowType(operand) ?: return false
            return isAssignable(realType, castType)
        }

        // check == and !=
        if (element is PsiBinaryExpression && (
            element.operationSign.tokenType == JavaTokenType.EQEQ ||
                element.operationSign.tokenType == JavaTokenType.NE
            )
        ) {
            val rightType = element.rOperand?.let(McdevDfaUtil::getTypeConstraint) ?: return false
            val leftType = McdevDfaUtil.getTypeConstraint(element.lOperand) ?: return false
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
        val realType = McdevDfaUtil.tryGetDataflowType(castExpression) ?: return false

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

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    companion object {
        private val INSPECTIONS = setOf("ConstantConditions", "ConstantValue", "DataFlowIssue")
    }
}
