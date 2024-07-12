/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiType
import com.intellij.uast.UastHintedVisitorAdapter
import com.intellij.uast.createUastSmartPointer
import com.siyeh.ig.psiutils.CommentTracker
import com.siyeh.ig.psiutils.MethodCallUtils
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.generate.replace
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.util.isMethodCall
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class WrongTypeInTranslationArgsInspection : TranslationInspection() {
    override fun getStaticDescription() = "Detect wrong argument types in translation arguments"

    private val typesHint: Array<Class<out UElement>> =
        arrayOf(UReferenceExpression::class.java, ULiteralExpression::class.java)

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor =
        UastHintedVisitorAdapter.create(holder.file.language, Visitor(holder), typesHint)

    private class Visitor(private val holder: ProblemsHolder) : AbstractUastNonRecursiveVisitor() {

        override fun visitElement(node: UElement): Boolean {
            if (node is UReferenceExpression) {
                doCheck(node)
            }

            return super.visitElement(node)
        }

        override fun visitLiteralExpression(node: ULiteralExpression): Boolean {
            doCheck(node)
            return super.visitLiteralExpression(node)
        }

        private fun doCheck(element: UElement) {
            val result = TranslationInstance.find(element)
            if (result == null || result.foldingElement !is UCallExpression || result.allowArbitraryArgs) {
                return
            }

            val args = result.foldingElement.valueArguments

            val javaCall = result.foldingElement.javaPsi as? PsiCall ?: return
            if (!MethodCallUtils.isVarArgCall(javaCall)) {
                return
            }

            val resolvedMethod = result.foldingElement.resolveToUElement() as? UMethod ?: return
            val parameters = resolvedMethod.uastParameters
            if ((parameters.lastOrNull()?.type as? PsiEllipsisType)
                ?.componentType?.equalsToText(CommonClassNames.JAVA_LANG_OBJECT) != true
            ) {
                return
            }
            val elementSourcePsi = element.sourcePsi ?: return
            val module = elementSourcePsi.findModule() ?: return
            val componentName = module.getMappedClass("net.minecraft.network.chat.Component")
            val translatableName = module.getMappedMethod(
                "net.minecraft.network.chat.Component",
                "translatable",
                "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            )
            val isComponentTranslatable = resolvedMethod.name == translatableName &&
                resolvedMethod.getContainingUClass()?.qualifiedName == componentName

            val resolveScope = elementSourcePsi.resolveScope
            val booleanType =
                PsiType.getTypeByName(CommonClassNames.JAVA_LANG_BOOLEAN, holder.project, resolveScope)
            val numberType =
                PsiType.getTypeByName(CommonClassNames.JAVA_LANG_NUMBER, holder.project, resolveScope)
            val stringType = PsiType.getJavaLangString(PsiManager.getInstance(holder.project), resolveScope)
            val componentType = PsiType.getTypeByName(componentName, holder.project, resolveScope)
            for (arg in args.drop(parameters.size - 1)) {
                val type = arg.getExpressionType() ?: continue
                if (!booleanType.isAssignableFrom(type) &&
                    !numberType.isAssignableFrom(type) &&
                    !stringType.isAssignableFrom(type) &&
                    !componentType.isAssignableFrom(type)
                ) {
                    var fixes = arrayOf<LocalQuickFix>(WrapWithStringValueOfFix(arg.sourcePsi!!))
                    if (isComponentTranslatable && result.foldingElement.isMethodCall()) {
                        val referenceName = result.foldingElement.methodIdentifier
                        if (referenceName != null) {
                            fixes = arrayOf<LocalQuickFix>(ReplaceWithTranslatableEscapedFix(referenceName)) + fixes
                        }
                    }
                    holder.registerProblem(
                        arg.sourcePsi!!,
                        "Translation argument is not a 'String', 'Number', 'Boolean' or 'Component'",
                        *fixes
                    )
                }
            }
        }
    }

    private class ReplaceWithTranslatableEscapedFix(
        identifier: UIdentifier
    ) : LocalQuickFix {

        @FileModifier.SafeFieldForPreview
        private val identifierPointer = identifier.createUastSmartPointer()

        override fun getFamilyName() = "Replace with 'Component.translatableEscaped'"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val identifier = identifierPointer.element ?: return
            val module = identifier.sourcePsi!!.findModule() ?: return
            val newMethodName = module.getMappedMethod(
                "net.minecraft.network.chat.Component",
                "translatableEscape",
                "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            )
            val fakeSourcePsi = JavaPsiFacade.getElementFactory(project).createIdentifier(newMethodName)
            identifier.replace(UIdentifier(fakeSourcePsi, identifier.uastParent))
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
