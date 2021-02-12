/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INJECT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethodCallExpression

class MixinCancellableInspection : MixinInspection() {

    override fun getStaticDescription(): String =
        "@Inject must be cancellable in order to be cancelled"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            if (expression.findContainingClass()?.isMixin == false) {
                return
            }

            val calledMethod = expression.resolveMethod() ?: return
            if (calledMethod.name != "cancel" && calledMethod.name != "setReturnValue") {
                return
            }

            val classFqn = calledMethod.containingClass?.fullQualifiedName ?: return
            if (classFqn != CALLBACK_INFO && classFqn != CALLBACK_INFO_RETURNABLE) {
                return
            }

            val injectAnnotation = expression.findContainingMethod()?.getAnnotation(INJECT) ?: return
            val cancellableValue = injectAnnotation.findAttributeValue("cancellable")
            if ((cancellableValue as? PsiLiteral)?.value != true) {
                holder.registerProblem(
                    expression,
                    "@Inject must be marked as cancellable in order to be cancelled",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    MakeInjectCancellableFix(injectAnnotation)
                )
            }
        }
    }

    private class MakeInjectCancellableFix(element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {

        override fun getFamilyName(): String = "Mark as cancellable"

        override fun getText(): String = familyName

        override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            val annotation = startElement as PsiAnnotation
            val value = PsiElementFactory.getInstance(project).createExpressionFromText("true", annotation)
            annotation.setDeclaredAttributeValue("cancellable", value)
        }
    }
}
