/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.reflection.reference

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.simpleQualifiedMemberReference
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.hint.HintManager
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.ProcessingContext

object ReflectedFieldReference : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // The pattern for this provider should only match method params, but better be safe
        if (element.parent !is PsiExpressionList) {
            return arrayOf()
        }
        return arrayOf(Reference(element as PsiLiteral))
    }

    class Reference(element: PsiLiteral) : PsiReferenceBase.Poly<PsiLiteral>(element) {
        val fieldName
            get() = element.constantStringValue ?: ""

        override fun getVariants(): Array<Any> {
            val typeClass = findReferencedClass() ?: return arrayOf()

            return typeClass.allFields.filter { !it.hasModifier(JvmModifier.PUBLIC) }
                .map { field ->
                    JavaLookupElementBuilder.forField(field).withInsertHandler { context, _ ->
                        val literal = context.file.findElementAt(context.startOffset)?.parent as? PsiLiteral ?: return@withInsertHandler

                        val srgManager = literal.findModule()?.let { MinecraftFacet.getInstance(it) }?.getModuleOfType(McpModuleType)?.srgManager
                        val srgMap = srgManager?.srgMapNow
                        val srgField = srgMap?.getSrgField(field.simpleQualifiedMemberReference) ?: return@withInsertHandler

                        context.setLaterRunnable {
                            // Commit changes made by code completion
                            context.commitDocument()

                            // Run command to replace PsiElement
                            CommandProcessor.getInstance().runUndoTransparentAction {
                                runWriteAction {
                                    val elementFactory = JavaPsiFacade.getElementFactory(context.project)
                                    val srgLiteral = elementFactory.createExpressionFromText("\"${srgField.name}\"", literal.parent)
                                    literal.parent.addBefore(srgLiteral, literal)
                                    CodeStyleManager.getInstance(context.project).reformat(literal.parent, true)
                                }
                            }
                        }
                    }
                }
                .toTypedArray()
        }

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            val typeClass = findReferencedClass() ?: return arrayOf()

            val srgManager = element.findModule()?.let { MinecraftFacet.getInstance(it) }?.getModuleOfType(McpModuleType)?.srgManager
            val srgMap = srgManager?.srgMapNow
            val mcpName = srgMap?.mapSrgName(fieldName)

            if (mcpName != null) {
                return typeClass.allFields.filter { it.name == mcpName }.map(::PsiElementResolveResult).toTypedArray()
            }

            return typeClass.allFields.filter { it.name == fieldName }.map(::PsiElementResolveResult).toTypedArray()
        }

        private fun findReferencedClass(): PsiClass? {
            val callParams = element.parent as? PsiExpressionList
            val classRef = callParams?.expressions?.first() as? PsiClassObjectAccessExpression
            val type = classRef?.operand?.type as? PsiClassType
            return type?.resolve()
        }
    }
}
