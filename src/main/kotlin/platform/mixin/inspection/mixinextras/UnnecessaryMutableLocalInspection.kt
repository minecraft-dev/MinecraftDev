/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.WrapOperationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.unwrapLocalRef
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType
import com.siyeh.ig.psiutils.MethodCallUtils
import org.jetbrains.plugins.groovy.intentions.style.inference.resolve

class UnnecessaryMutableLocalInspection : MixinInspection() {
    override fun getStaticDescription() = "Unnecessary mutable reference to captured local"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitMethod(method: PsiMethod) {
            val project = method.project
            val hasValidMixinAnnotation = method.annotations.any { ann ->
                MixinAnnotationHandler.forMixinAnnotation(ann, project)
                    // Mutable Local references do have different semantics inside a WrapOperation.
                    ?.let { it is InjectorAnnotationHandler && it !is WrapOperationHandler } == true
            }
            if (!hasValidMixinAnnotation) {
                return
            }

            // ignore if method has any references
            val hasReferences = ReferencesSearch.search(method)
                .mapNotNull { PsiUtil.skipParenthesizedExprUp(it.element).parent as? PsiMethodCallExpression }
                .any { !MethodCallUtils.hasSuperQualifier(it) }
            if (hasReferences) {
                return
            }

            for ((i, param) in method.parameterList.parameters.withIndex()) {
                if (!param.hasAnnotation(MixinConstants.MixinExtras.LOCAL)) {
                    continue
                }
                val paramType = param.type.resolve()
                if (paramType?.qualifiedName?.startsWith(MixinConstants.MixinExtras.LOCAL_REF_PACKAGE) != true) {
                    continue
                }

                checkParameter(holder, method, param, i, paramType)
            }
        }
    }

    private fun checkParameter(
        holder: ProblemsHolder,
        originalMethod: PsiMethod,
        originalParam: PsiParameter,
        paramIndex: Int,
        paramType: PsiClass
    ) {
        var hasAnyGets = false
        for (method in OverridingMethodsSearch.search(originalMethod).findAll() + listOf(originalMethod)) {
            val param = method.parameterList.getParameter(paramIndex) ?: return
            val getMethod = paramType.findMethodsByName("get", false).firstOrNull() ?: return
            for (ref in ReferencesSearch.search(param)) {
                if (isDelegationToSuper(ref.element, paramIndex)) {
                    continue
                }
                val parent = PsiUtil.skipParenthesizedExprUp(ref.element.parent) as? PsiReferenceExpression ?: return
                if (parent.references.any { it.isReferenceTo(getMethod) }) {
                    hasAnyGets = true
                } else {
                    return
                }
            }
        }
        if (!hasAnyGets) {
            // Don't annoy them if they've just made the parameter
            return
        }
        holder.registerProblem(
            originalParam.typeElement ?: originalParam,
            "@Local could be captured immutably",
            SwitchToImmutableCaptureFix(originalParam)
        )
    }

    // Ignore super delegations in subclasses. super.foo(myLocalRef) has no effect on whether the local can be converted
    private fun isDelegationToSuper(ref: PsiElement, paramIndex: Int): Boolean {
        val method = ref.findContainingMethod() ?: return false
        val superMethod = method.findSuperMethods().firstOrNull { it.containingClass?.isInterface == false }
            ?: return false

        // For some reason ref is sometimes the identifier rather than the reference expression. Get the reference expr
        val actualRef = if (ref is PsiReferenceExpression) {
            ref
        } else {
            PsiUtil.skipParenthesizedExprUp(ref.parent) as? PsiReferenceExpression ?: return false
        }
        val param = PsiUtil.skipParenthesizedExprUp(actualRef)
        val paramList = param.parent as? PsiExpressionList ?: return false
        val methodCall = paramList.parent as? PsiMethodCallExpression ?: return false

        // Check that the method call is a super call
        if (!MethodCallUtils.hasSuperQualifier(methodCall)) {
            return false
        }

        // Check that our reference is in the correct parameter index
        if (paramList.expressions.getOrNull(paramIndex) != param) {
            return false
        }

        // Check that the super call is referencing the correct super method.
        return methodCall.resolveMethod() == superMethod
    }

    private class SwitchToImmutableCaptureFix(param: PsiParameter) : LocalQuickFixOnPsiElement(param) {
        override fun getFamilyName() = "Switch to immutable capture"
        override fun getText() = "Switch to immutable capture"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val param = startElement as? PsiParameter ?: return
            val method = param.parentOfType<PsiMethod>() ?: return
            val paramIndex = method.parameterList.getParameterIndex(param)
            val methods = mutableListOf(method)
            if (file.isPhysical) {
                methods.addAll(OverridingMethodsSearch.search(method))
            }
            for (impl in methods) {
                fixMethod(impl, paramIndex)
            }
        }

        private fun fixMethod(method: PsiMethod, paramIndex: Int) {
            val param = method.parameterList.getParameter(paramIndex) ?: return
            val innerType = param.type.unwrapLocalRef()
            val factory = PsiElementFactory.getInstance(method.project)
            param.typeElement?.replace(factory.createTypeElement(innerType))
            for (ref in ReferencesSearch.search(param)) {
                val refExpression = PsiUtil.skipParenthesizedExprUp(ref.element.parent) as? PsiReferenceExpression
                    ?: continue
                val call = refExpression.parent as? PsiMethodCallExpression ?: continue
                call.replace(ref.element)
            }
        }
    }
}
