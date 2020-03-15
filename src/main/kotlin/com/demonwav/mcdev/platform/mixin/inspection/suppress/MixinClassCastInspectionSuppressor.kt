/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.suppress

import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiThisExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.search.GlobalSearchScope

/**
 * Looks for `(SomeClass) (Object) this` expressions and suppresses the `ConstantConditions` inspection on it.
 */
class MixinClassCastInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION) {
            return false
        }

        val containingClass = element.findContainingClass() ?: return false
        val targets = containingClass.mixinTargets

        if (targets.isEmpty()) {
            return false
        }

        val castExpression = element.parent as? PsiTypeCastExpression ?: return false

        val castType = castExpression.castType ?: return false

        val project = element.project
        val factory = JavaPsiFacade.getInstance(project).elementFactory

        val toType = castType.type
        if (
            targets.none { t ->
                val type = factory.createType(t)
                // type == toType                   --> direct cast
                // toType.superTypes.contains(type) --> cast to a subclass of the current mixin
                // t.superTypes.contains(toType)    --> cast to a superclass of the current mixin
                type == toType || toType.superTypes.contains(type) || t.superTypes.contains(toType)
            }
        ) {
            return false
        }

        // we're looking for (SomeClass) (Object) this
        // So the operand of the first cast `(SomeClass)` must be `(Object) this`, another cast
        val operand = castExpression.operand as? PsiTypeCastExpression ?: return false

        if (operand.castType?.type != PsiType.getTypeByName(
                CommonClassNames.JAVA_LANG_OBJECT,
                project,
                GlobalSearchScope.allScope(project)
            )
        ) {
            return false
        }

        // If the operand of the operand is `this`, then this is the inspection we want to suppress
        return operand.operand is PsiThisExpression
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    companion object {
        private const val INSPECTION = "ConstantConditions"
    }
}
