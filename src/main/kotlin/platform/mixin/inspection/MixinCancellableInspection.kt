/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INJECT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtil

class MixinCancellableInspection : MixinInspection() {

    override fun getStaticDescription(): String =
        "Reports missing or unused cancellable @Inject usages"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitMethod(method: PsiMethod) {
            val injectAnnotation = method.getAnnotation(INJECT) ?: return

            val cancellableAttribute = injectAnnotation.findAttributeValue("cancellable") as? PsiLiteral ?: return
            val isCancellable = cancellableAttribute.value == true

            val ciParam = method.parameterList.parameters.firstOrNull {
                val className = (it.type as? PsiClassType)?.fullQualifiedName ?: return@firstOrNull false
                className == CALLBACK_INFO || className == CALLBACK_INFO_RETURNABLE
            } ?: return

            val ciType = (ciParam.type as? PsiClassType)?.resolve() ?: return
            val searchingFor = ciType.findMethodsByName("setReturnValue", false).firstOrNull()
                ?: ciType.findMethodsByName("cancel", false).firstOrNull()
                ?: return

            var mayUseCancel = false
            var definitelyUsesCancel = false
            for (ref in ReferencesSearch.search(ciParam)) {
                val parent = PsiUtil.skipParenthesizedExprUp(ref.element.parent)
                if (parent is PsiExpressionList) {
                    // method argument, we can't tell whether it uses cancel
                    mayUseCancel = true
                }
                val methodCall = parent as? PsiReferenceExpression ?: continue
                if (methodCall.references.any { it.isReferenceTo(searchingFor) }) {
                    definitelyUsesCancel = true
                    break
                }
            }

            if (definitelyUsesCancel && !isCancellable) {
                holder.registerProblem(
                    method.nameIdentifier ?: method,
                    "@Inject must be marked as cancellable in order to be cancelled",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    MakeInjectCancellableFix(injectAnnotation)
                )
            } else if (!definitelyUsesCancel && !mayUseCancel && isCancellable) {
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
