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

package com.demonwav.mcdev.platform.mixin.handlers.desugar

import com.demonwav.mcdev.util.childrenOfType
import com.demonwav.mcdev.util.hasSyntheticMethod
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.LambdaUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.refactoring.util.LambdaRefactoringUtil

object MethodReferenceToLambdaDesugarer : Desugarer() {
    private val QUALIFIED_METHOD_REFERENCE_KEY = Key.create<Boolean>("mcdev.desugar.methodReferenceToLambda.qualifiedMethodReference")

    fun isQualifiedMethodReference(lambda: PsiLambdaExpression): Boolean {
        return lambda.getCopyableUserData(QUALIFIED_METHOD_REFERENCE_KEY) == true
    }

    override fun desugar(project: Project, file: PsiJavaFile, context: DesugarContext) {
        for (methodRef in file.childrenOfType<PsiMethodReferenceExpression>()) {
            if (methodRef.hasSyntheticMethod(context.classVersion)) {
                desugarMethodReferenceToLambda(methodRef)
            }
        }
    }

    private fun desugarMethodReferenceToLambda(methodReference: PsiMethodReferenceExpression): PsiLambdaExpression? {
        val qualifierExpression = methodReference.qualifierExpression?.takeIf {
            it !is PsiReferenceExpression || it.resolve() !is PsiClass
        }?.copy()
        val originalMethodRef = DesugarUtil.getOriginalElement(methodReference)
        val originalMethodName = methodReference.referenceNameElement?.let(DesugarUtil::getOriginalElement)

        val lambda = LambdaRefactoringUtil.convertMethodReferenceToLambda(methodReference, false, true)
            ?: return null

        DesugarUtil.setOriginalElement(lambda, originalMethodRef)
        lambda.putCopyableUserData(QUALIFIED_METHOD_REFERENCE_KEY, qualifierExpression != null)
        for (parameter in lambda.parameterList.parameters) {
            DesugarUtil.setUnnamedVariable(parameter, true)
        }

        // convertMethodReferenceToLambda creates lambdas from text which loses the original elements. Add them back here
        val methodCall = LambdaUtil.extractSingleExpressionFromBody(lambda.body) as? PsiMethodCallExpression
        if (methodCall != null) {
            methodCall.methodExpression.referenceNameElement?.let { DesugarUtil.setOriginalElement(it, originalMethodName) }
            if (qualifierExpression != null) {
                methodCall.methodExpression.qualifierExpression?.replace(qualifierExpression)
            }
        }

        return lambda
    }
}
