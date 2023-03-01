/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.reflection.reference

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.simpleQualifiedMemberReference
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

    class Reference(element: PsiLiteral) : ReflectedMemberReferenceBasePoly(element) {
        val fieldName
            get() = element.constantStringValue ?: ""

        override val memberName: String
            get() = fieldName

        override fun getVariants(): Array<Any> {
            val typeClass = findReferencedClass() ?: return arrayOf()

            return typeClass.allFields
                .asSequence()
                .filter { !it.hasModifier(JvmModifier.PUBLIC) }
                .map { field ->
                    JavaLookupElementBuilder.forField(field).withInsertHandler { context, _ ->
                        val literal = context.file.findElementAt(context.startOffset)?.parent as? PsiLiteral
                            ?: return@withInsertHandler

                        val srgManager = literal.findModule()?.let { MinecraftFacet.getInstance(it) }
                            ?.getModuleOfType(McpModuleType)?.srgManager
                        val srgMap = srgManager?.srgMapNow
                        val srgField = srgMap?.getSrgField(field.simpleQualifiedMemberReference)
                            ?: return@withInsertHandler

                        context.setLaterRunnable {
                            // Commit changes made by code completion
                            context.commitDocument()

                            // Run command to replace PsiElement
                            CommandProcessor.getInstance().runUndoTransparentAction {
                                runWriteAction {
                                    val params = literal.parent
                                    val elementFactory = JavaPsiFacade.getElementFactory(context.project)
                                    val srgLiteral = elementFactory.createExpressionFromText(
                                        "\"${srgField.name}\"",
                                        literal.parent,
                                    )
                                    literal.replace(srgLiteral)

                                    CodeStyleManager.getInstance(context.project).reformat(srgLiteral.parent, true)

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
