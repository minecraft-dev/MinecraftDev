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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.WrapWithConditionHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.ContextAwareMethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.siyeh.ig.psiutils.VariableNameGenerator
import org.objectweb.asm.Type

class WrapWithConditionValidNonVoidInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when @WrapWithCondition targets a non-void instruction followed by a pop"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            val handler = MixinAnnotationHandler.forMixinAnnotation(annotation, holder.project) as? WrapWithConditionHandler ?: return

            val hasMatchingInstructions = handler.resolveInstructions(annotation).any { insnResult ->
                val validTargetType = handler.getValidTargetType(insnResult.result.insn)
                validTargetType != null && validTargetType != Type.VOID_TYPE
            }

            if (hasMatchingInstructions) {
                val fixes = mutableListOf<LocalQuickFix>()
                if (handler.getReplaceWithWrapOpInfo(annotation) != null) {
                    fixes += ReplaceWithWrapOpFix(annotation)
                }

                holder.registerProblem(
                    annotation.nameReferenceElement ?: annotation,
                    "@WrapWithCondition is targeting a non-void instruction",
                    *fixes.toTypedArray()
                )
            }
        }
    }

    private class ReplaceWithWrapOpFix(annotation: PsiAnnotation) : LocalQuickFixOnPsiElement(annotation) {
        override fun getFamilyName() = "Replace with @WrapOperation"
        override fun getText() = "Replace with @WrapOperation"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val annotation = startElement as? PsiAnnotation ?: return
            val handler = MixinAnnotationHandler.forMixinAnnotation(annotation, project) as? WrapWithConditionHandler ?: return
            val info = handler.getReplaceWithWrapOpInfo(annotation) ?: return
            val factory = JavaPsiFacade.getElementFactory(project)

            annotation.nameReferenceElement?.replace(factory.createReferenceElementByFQClassName(MixinConstants.MixinExtras.WRAP_OPERATION, annotation.resolveScope))

            val method = annotation.findContainingMethod() ?: return
            val operationName = addOperationParameter(method, factory, info)
            replaceReturnType(method, factory, info)
            replaceReturnStatements(method, factory, info, operationName)

            JavaCodeStyleManager.getInstance(project).shortenClassReferences(method)
            CodeStyleManager.getInstance(project).reformat(method)
        }

        private fun addOperationParameter(method: PsiMethod, factory: PsiElementFactory, info: ReplaceWithWrapOpInfo): String {
            val operationName = VariableNameGenerator(method, VariableKind.PARAMETER)
                .byName("original")
                .generate(true)
            val parameterType = factory.createTypeFromText(
                "${MixinConstants.MixinExtras.OPERATION}<${
                    PsiTypesUtil.boxIfPossible(info.targetType.toPsiType(
                        factory,
                        method
                    ).canonicalText)
                }>",
                method
            )
            val parameter = factory.createParameter(operationName, parameterType)
            method.parameterList.addBefore(
                parameter,
                method.parameterList.parameters.getOrNull(info.requiredParameterCount)
            )
            return operationName
        }

        private fun replaceReturnType(method: PsiMethod, factory: PsiElementFactory, info: ReplaceWithWrapOpInfo) {
            method.returnTypeElement?.replace(factory.createTypeElement(info.targetType.toPsiType(factory, method)))
        }

        private fun replaceReturnStatements(method: PsiMethod, factory: PsiElementFactory, info: ReplaceWithWrapOpInfo, operationName: String) {
            val returnType = info.targetType.toPsiType(factory, method)

            for (returnStatement in PsiUtil.findReturnStatements(method)) {
                val returnValue = returnStatement.returnValue ?: continue
                if (returnValue is PsiLiteralExpression) {
                    val literalValue = returnValue.value
                    if (literalValue == true) {
                        returnValue.replace(createOperationCall(method, factory, info, operationName))
                        continue
                    } else if (literalValue == false) {
                        returnValue.replace(factory.createExpressionFromText(PsiTypesUtil.getDefaultValueOfType(returnType, true), method))
                        continue
                    }
                }

                val ifStatement = factory.createStatementFromText(
                    "if (cond) { return x; } else { return ${PsiTypesUtil.getDefaultValueOfType(returnType, true)}; }",
                    method
                ) as PsiIfStatement
                ifStatement.condition!!.replace(returnValue)
                PsiUtil.findReturnStatements((ifStatement.thenBranch as PsiBlockStatement).codeBlock)
                    .first()
                    .returnValue!!
                    .replace(createOperationCall(method, factory, info, operationName))
                returnStatement.replace(ifStatement)
            }
        }

        private fun createOperationCall(method: PsiMethod, factory: PsiElementFactory, info: ReplaceWithWrapOpInfo, operationName: String): PsiExpression {
            val expressionText = buildString {
                append(operationName)
                append(".call(")
                for ((i, param) in method.parameterList.parameters.take(info.requiredParameterCount).withIndex()) {
                    if (i != 0) {
                        append(", ")
                    }
                    append(param.name)
                }
                append(")")
            }
            return factory.createExpressionFromText(expressionText, method)
        }
    }
}

private class ReplaceWithWrapOpInfo(val targetType: Type, val requiredParameterCount: Int)

private fun WrapWithConditionHandler.getReplaceWithWrapOpInfo(annotation: PsiAnnotation): ReplaceWithWrapOpInfo? {
    var targetType: Type? = null
    var requiredParameterCount: Int? = null

    for (targetMember in MixinAnnotationHandler.resolveTarget(annotation)) {
        if (targetMember !is MethodTargetMember) {
            continue
        }

        val targetMethod = targetMember.classAndMethod
        for (insn in resolveInstructions(
            annotation,
            targetMethod.clazz,
            targetMethod.method,
            (targetMember as? ContextAwareMethodTargetMember)?.selector
        )) {
            val newTargetType = getValidTargetType(insn.insn) ?: continue
            if (targetType != null && targetType != newTargetType) {
                return null
            }
            targetType = newTargetType

            val newRequiredParameterCount = getRequiredParameterCount(insn.insn, targetMethod.clazz, annotation)
            if (requiredParameterCount != null && requiredParameterCount != newRequiredParameterCount) {
                return null
            }
            requiredParameterCount = newRequiredParameterCount
        }
    }

    if (targetType == null || requiredParameterCount == null) {
        return null
    }

    return ReplaceWithWrapOpInfo(targetType, requiredParameterCount)
}
