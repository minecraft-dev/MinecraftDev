/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
                            ImplicitConstructorInvokerQuickFix(invokerAnnotation)
                        )
                    }
                }
            }
        }
    }

    override fun getStaticDescription() = "Implicit constructor invoker (fails outside of dev)"

    private class ImplicitConstructorInvokerQuickFix(
        annotation: PsiAnnotation
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
