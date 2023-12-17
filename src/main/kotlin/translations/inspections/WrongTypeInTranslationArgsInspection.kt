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

package com.demonwav.mcdev.translations.inspections

import com.demonwav.mcdev.platform.mcp.mappings.getMappedClass
import com.demonwav.mcdev.platform.mcp.mappings.getMappedMethod
import com.demonwav.mcdev.translations.identification.TranslationInstance
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.siyeh.ig.psiutils.CommentTracker
import com.siyeh.ig.psiutils.MethodCallUtils

class WrongTypeInTranslationArgsInspection : TranslationInspection() {
    override fun getStaticDescription() = "Detect wrong argument types in translation arguments"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            doCheck(expression)
        }

        override fun visitLiteralExpression(expression: PsiLiteralExpression) {
            doCheck(expression)
        }

        private fun doCheck(element: PsiElement) {
            val result = TranslationInstance.find(element)
            if (result == null || result.foldingElement !is PsiCall || result.allowArbitraryArgs) {
                return
            }

            val args = result.foldingElement.argumentList ?: return

            if (!MethodCallUtils.isVarArgCall(result.foldingElement)) {
                return
            }

            val resolvedMethod = result.foldingElement.resolveMethod() ?: return
            if ((resolvedMethod.parameterList.parameters.lastOrNull()?.type as? PsiEllipsisType)
                ?.componentType?.equalsToText(CommonClassNames.JAVA_LANG_OBJECT) != true
            ) {
                return
            }
            val module = element.findModule() ?: return
            val componentName = module.getMappedClass("net.minecraft.network.chat.Component")
            val translatableName = module.getMappedMethod(
                "net.minecraft.network.chat.Component",
                "translatable",
                "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            )
            val isComponentTranslatable = resolvedMethod.name == translatableName &&
                resolvedMethod.containingClass?.qualifiedName == componentName

            val booleanType =
                PsiType.getTypeByName(CommonClassNames.JAVA_LANG_BOOLEAN, holder.project, element.resolveScope)
            val numberType =
                PsiType.getTypeByName(CommonClassNames.JAVA_LANG_NUMBER, holder.project, element.resolveScope)
            val stringType = PsiType.getJavaLangString(PsiManager.getInstance(holder.project), element.resolveScope)
            val componentType = PsiType.getTypeByName(componentName, holder.project, element.resolveScope)
            for (arg in args.expressions.drop(resolvedMethod.parameterList.parametersCount - 1)) {
                val type = arg.type ?: continue
                if (!booleanType.isAssignableFrom(type) &&
                    !numberType.isAssignableFrom(type) &&
                    !stringType.isAssignableFrom(type) &&
                    !componentType.isAssignableFrom(type)
                ) {
                    var fixes = arrayOf<LocalQuickFix>(WrapWithStringValueOfFix(arg))
                    if (isComponentTranslatable && result.foldingElement is PsiMethodCallExpression) {
                        val referenceName = result.foldingElement.methodExpression.referenceNameElement
                        if (referenceName != null) {
                            fixes = arrayOf<LocalQuickFix>(ReplaceWithTranslatableEscapedFix(referenceName)) + fixes
                        }
                    }
                    holder.registerProblem(
                        arg,
                        "Translation argument is not a 'String', 'Number', 'Boolean' or 'Component'",
                        *fixes
                    )
                }
            }
        }
    }

    private class ReplaceWithTranslatableEscapedFix(
        referenceName: PsiElement
    ) : LocalQuickFixOnPsiElement(referenceName) {
        override fun getFamilyName() = "Replace with 'Component.translatableEscaped'"
        override fun getText() = "Replace with 'Component.translatableEscaped'"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val module = startElement.findModule() ?: return
            val newMethodName = module.getMappedMethod(
                "net.minecraft.network.chat.Component",
                "translatableEscape",
                "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            )
            startElement.replace(JavaPsiFacade.getElementFactory(project).createIdentifier(newMethodName))
        }
    }

    private class WrapWithStringValueOfFix(element: PsiElement) : LocalQuickFixOnPsiElement(element) {
        override fun getFamilyName() = "Wrap with 'String.valueOf()'"
        override fun getText() = "Wrap with 'String.valueOf()'"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val ct = CommentTracker()
            ct.replace(startElement, "String.valueOf(${ct.text(startElement)})")
        }
    }
}
