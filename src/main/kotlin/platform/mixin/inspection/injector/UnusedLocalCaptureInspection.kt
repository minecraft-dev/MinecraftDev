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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil

class UnusedLocalCaptureInspection : MixinInspection() {
    object Util {
        fun findCallbackInfoParam(parameters: Array<PsiParameter>): Int {
            return parameters.indexOfFirst { param ->
                val classType = param.type as? PsiClassType ?: return@indexOfFirst false
                val className = classType.className
                if (className != "CallbackInfo" && className != "CallbackInfoReturnable") {
                    return@indexOfFirst false
                }
                val qualifiedName = classType.resolve()?.qualifiedName ?: return@indexOfFirst false
                qualifiedName == MixinConstants.Classes.CALLBACK_INFO ||
                    qualifiedName == MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
            }
        }
    }

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.Annotations.INJECT)) {
                return
            }

            // check that we are capturing locals
            val localsValue =
                PsiUtil.skipParenthesizedExprDown(
                    annotation.findDeclaredAttributeValue("locals") as? PsiExpression
                ) as? PsiReferenceExpression ?: return
            if (localsValue.referenceName == "NO_CAPTURE") {
                return
            }
            val enumName = (localsValue.resolve() as? PsiEnumConstant)?.containingClass?.qualifiedName
            if (enumName != MixinConstants.Classes.LOCAL_CAPTURE) {
                return
            }

            val method = annotation.findContainingMethod() ?: return

            if (OverridingMethodsSearch.search(method).any()) {
                return
            }

            // find the start of the locals in the parameter list
            val parameters = method.parameterList.parameters
            val callbackInfoIndex = Util.findCallbackInfoParam(parameters)
            if (callbackInfoIndex == -1) {
                return
            }

            val hasAnyUsedLocals = parameters.asSequence().drop(callbackInfoIndex + 1).any { param ->
                ReferencesSearch.search(param).anyMatch {
                    !it.isSoft && !PsiTreeUtil.isAncestor(param, it.element, false)
                }
            }
            if (!hasAnyUsedLocals) {
                holder.registerProblem(
                    localsValue,
                    "Unused @Inject local capture",
                    RemoveLocalCaptureFix(annotation, callbackInfoIndex)
                )
            }
        }
    }

    private class RemoveLocalCaptureFix(
        injectAnnotation: PsiAnnotation,
        private val callbackInfoIndex: Int
    ) : LocalQuickFixOnPsiElement(injectAnnotation) {
        override fun getFamilyName() = "Remove @Inject local capture"

        override fun getText() = "Remove @Inject local capture"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val injectAnnotation = startElement as? PsiAnnotation ?: return
            val method = injectAnnotation.findContainingMethod() ?: return
            method.parameterList.parameters.asSequence().drop(callbackInfoIndex + 1).forEach(PsiElement::delete)
            AnnotationAttributeFix(injectAnnotation, "locals" to null).applyFix()
        }
    }
}
