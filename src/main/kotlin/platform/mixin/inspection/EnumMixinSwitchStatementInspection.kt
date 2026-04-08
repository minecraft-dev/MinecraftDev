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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiSwitchBlock
import com.intellij.psi.PsiSwitchExpression
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTypesUtil

class EnumMixinSwitchStatementInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when a switch statement or expression is used on an enum mixin"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitSwitchStatement(statement: PsiSwitchStatement) {
            visitSwitchBlock(statement)
        }

        override fun visitSwitchExpression(expression: PsiSwitchExpression) {
            visitSwitchBlock(expression)
        }

        private fun visitSwitchBlock(block: PsiSwitchBlock) {
            val expression = block.expression ?: return
            val mixinClass = PsiTypesUtil.getPsiClass(expression.type) ?: return
            if (!mixinClass.isEnum || !mixinClass.isMixin) {
                return
            }

            val fixes = mutableListOf<LocalQuickFix>()
            val targetClass = mixinClass.mixinTargets.singleOrNull()
            if (targetClass != null) {
                fixes += AddCastFix(expression, targetClass.name)
            }
            holder.registerProblem(expression, "Switch used on enum mixin", *fixes.toTypedArray())
        }
    }

    private class AddCastFix(expression: PsiExpression, private val targetClass: String) : LocalQuickFixOnPsiElement(expression) {
        override fun getFamilyName() = "Add double cast to ${targetClass.substringAfterLast('/').replace('$', '.')}"
        override fun getText() = "Add cast to target class ${targetClass.substringAfterLast('/').replace('$', '.')}"

        override fun invoke(project: Project, psiFile: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val expression = startElement as? PsiExpression ?: return
            val doubleCast = JavaPsiFacade.getElementFactory(project).createExpressionFromText(
                "(${targetClass.replace('/', '.').replace('$', '.')}) (java.lang.Object) x",
                expression
            ) as PsiTypeCastExpression
            (doubleCast.operand as PsiTypeCastExpression).operand!!.replace(expression)
            val resultingExpression = expression.replace(doubleCast)
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(resultingExpression)
        }
    }
}
