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

package com.demonwav.mcdev.platform.forge.reflection.reference

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.ProcessingContext

object ReflectedMethodReference : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // The pattern for this provider should only match method params, but better be safe
        if (element.parent !is PsiExpressionList) {
            return arrayOf()
        }
        return arrayOf(Reference(element as PsiLiteral))
    }

    class Reference(element: PsiLiteral) : ReflectedMemberReferenceBasePoly(element) {
        val methodName
            get() = element.constantStringValue ?: ""

        override val memberName: String
            get() = methodName

        val expressionList
            get() = element.parent as PsiExpressionList

        override fun getVariants(): Array<Any> {
            val typeClass = findReferencedClass() ?: return arrayOf()

            return typeClass.allMethods
                .asSequence()
                .filter { !it.hasModifier(JvmModifier.PUBLIC) }
                .map { method ->
                    JavaLookupElementBuilder.forMethod(method, PsiSubstitutor.EMPTY).withInsertHandler { context, _ ->
                        val literal = context.file.findElementAt(context.startOffset)?.parent as? PsiLiteral
                            ?: return@withInsertHandler
                        val params = literal.parent as? PsiExpressionList ?: return@withInsertHandler
                        val srgManager = literal.findModule()?.let { MinecraftFacet.getInstance(it) }
                            ?.getModuleOfType(McpModuleType)?.srgManager
                        val srgMap = srgManager?.srgMapNow

                        val signature = method.getSignature(PsiSubstitutor.EMPTY)
                        val returnType = method.returnType?.let { TypeConversionUtil.erasure(it).canonicalText }
                            ?: return@withInsertHandler
                        val paramTypes = MethodSignatureUtil.calcErasedParameterTypes(signature)
                            .map { it.canonicalText }

                        val memberRef = method.qualifiedMemberReference
                        val srgMethod = srgMap?.getSrgMethod(memberRef) ?: memberRef

                        context.setLaterRunnable {
                            // Commit changes made by code completion
                            context.commitDocument()

                            // Run command to replace PsiElement
                            CommandProcessor.getInstance().runUndoTransparentAction {
                                runWriteAction {
                                    val elementFactory = JavaPsiFacade.getElementFactory(context.project)
                                    val srgLiteral = elementFactory.createExpressionFromText(
                                        "\"${srgMethod.name}\"",
                                        params,
                                    )

                                    if (params.expressionCount > 1) {
                                        params.expressions[1].replace(srgLiteral)
                                    } else {
                                        params.add(srgLiteral)
                                    }

                                    if (params.expressionCount > 2) {
                                        params.deleteChildRange(params.expressions[2], params.expressions.last())
                                    }
                                    val returnTypeRef = elementFactory.createExpressionFromText(
                                        "$returnType.class",
                                        params,
                                    )
                                    params.add(returnTypeRef)

                                    for (paramType in paramTypes) {
                                        val paramTypeRef = elementFactory.createExpressionFromText(
                                            "$paramType.class",
                                            params,
                                        )
                                        params.add(paramTypeRef)
                                    }

                                    JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(params)
                                    CodeStyleManager.getInstance(context.project).reformat(params, true)

                                    context.editor.caretModel.moveToOffset(params.textRange.endOffset)
                                }
                            }
                        }
                    }
                }
                .toTypedArray()
        }
    }
}
