/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference

class MixinContributedReferenceInspection : BaseJavaBatchLocalInspectionTool() {

    override fun getStaticDescription(): String? {
        return "Reports references to unresolved Mixin elements. These will likely fail at runtime."
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return MethodReferenceVisitor(holder)
    }
}

private class MethodReferenceVisitor(val holder: ProblemsHolder) : JavaElementVisitor() {

    override fun visitLiteralExpression(expression: PsiLiteralExpression?) {
        expression ?: return
        if (expression.value !is String) {
            return
        }

        for (reference in expression.references) {
            if (reference is MixinReference) {
                if (cannotResolve(reference)) {
                    holder.registerProblem(reference, "Cannot resolve ${reference.description}" , ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                }
            }
        }
    }

}

private fun cannotResolve(reference: PsiReference): Boolean {
    if (reference is PsiPolyVariantReference) {
        return reference.multiResolve(false).isEmpty()
    } else {
        return reference.resolve() == null
    }
}
