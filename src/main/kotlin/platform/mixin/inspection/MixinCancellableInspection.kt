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
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.siyeh.ig.psiutils.ControlFlowUtils.elementContainsCallToMethod

class MixinCancellableInspection : MixinInspection() {

    override fun getStaticDescription(): String =
        "Reports missing or unused cancellable @Inject usages"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitMethod(method: PsiMethod) {
            val injectAnnotation = method.getAnnotation(INJECT) ?: return

            val cancellableAttribute = injectAnnotation.findAttributeValue("cancellable") as? PsiLiteral ?: return
            val isCancellable = cancellableAttribute.value == true

            val usesCancel = elementContainsCallToMethod(method, CALLBACK_INFO, PsiType.VOID, "cancel") ||
                elementContainsCallToMethod(method, CALLBACK_INFO_RETURNABLE, PsiType.VOID, "setReturnValue", null)

            if (usesCancel && !isCancellable) {
                holder.registerProblem(
                    method.nameIdentifier ?: method,
                    "@Inject must be marked as cancellable in order to be cancelled",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    MakeInjectCancellableFix(injectAnnotation)
                )
            } else if (!usesCancel && isCancellable) {
                holder.registerProblem(
                    cancellableAttribute.parent,
                    "@Inject is cancellable but is never cancelled",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    RemoveInjectCancellableFix(injectAnnotation)
                )
            }
        }
    }

    private class MakeInjectCancellableFix(element: PsiAnnotation) :
        LocalQuickFixAndIntentionActionOnPsiElement(element) {

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

    private class RemoveInjectCancellableFix(element: PsiAnnotation) :
        LocalQuickFixAndIntentionActionOnPsiElement(element) {

        override fun getFamilyName(): String = "Remove unused cancellable attribute"

        override fun getText(): String = familyName

        override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            val annotation = startElement as PsiAnnotation
            annotation.setDeclaredAttributeValue("cancellable", null)
        }
    }
}
