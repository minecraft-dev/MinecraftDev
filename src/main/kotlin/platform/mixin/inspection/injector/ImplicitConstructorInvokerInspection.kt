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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod

class ImplicitConstructorInvokerInspection : MixinInspection() {
    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                val invokerAnnotation = method.findAnnotation(INVOKER) ?: return
                if (invokerAnnotation.findDeclaredAttributeValue("value")?.constantStringValue.isNullOrEmpty()) {
                    if (method.name.let { it.startsWith("create") || it.startsWith("new") }) {
                        holder.registerProblem(
                            invokerAnnotation.nameReferenceElement ?: return,
                            "Implicit constructor invokers should be explicit (fails outside of dev)",
                            ImplicitConstructorInvokerQuickFix(invokerAnnotation),
                        )
                    }
                }
            }
        }
    }

    override fun getStaticDescription() = "Implicit constructor invoker (fails outside of dev)"

    private class ImplicitConstructorInvokerQuickFix(
        annotation: PsiAnnotation,
    ) : LocalQuickFixOnPsiElement(annotation) {
        override fun getFamilyName() = "Make constructor invoker explicit"
        override fun getText() = "Make constructor invoker explicit"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val annotation = startElement as? PsiAnnotation ?: return
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val initExpr = elementFactory.createExpressionFromText("\"<init>\"", null)
            annotation.setDeclaredAttributeValue("value", initExpr)
        }
    }
}
