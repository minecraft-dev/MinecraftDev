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

package com.demonwav.mcdev.util.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext

/**
 * Provides a simple [PsiReference] implementation for a global
 * [ReferenceResolver].
 */
abstract class ReferenceResolver : PsiReferenceProvider() {

    protected abstract fun resolveReference(context: PsiElement): PsiElement?
    protected abstract fun collectVariants(context: PsiElement): Array<Any>

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> =
        arrayOf(Reference(element as PsiLiteral, this))

    private class Reference(element: PsiLiteral, private val resolver: ReferenceResolver) :
        PsiReferenceBase<PsiLiteral>(element) {

        override fun resolve(): PsiElement? {
            val context = element.findContextElement()
            if (context.isValid) {
                return resolver.resolveReference(context)
            }
            return null
        }

        override fun getVariants(): Array<Any> {
            val context = element.findContextElement()
            if (context.isValid) {
                return resolver.collectVariants(element.findContextElement())
            }
            return ArrayUtil.EMPTY_OBJECT_ARRAY
        }
    }
}

/**
 * Provides a simple [com.intellij.psi.PsiPolyVariantReference] implementation
 * for a global [ReferenceResolver].
 */
abstract class PolyReferenceResolver : PsiReferenceProvider() {

    protected abstract fun resolveReference(context: PsiElement): Array<ResolveResult>
    protected abstract fun collectVariants(context: PsiElement): Array<Any>

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> =
        arrayOf(Reference(element as PsiLiteral, this))

    private class Reference(element: PsiLiteral, private val resolver: PolyReferenceResolver) :
        PsiReferenceBase.Poly<PsiLiteral>(element) {

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            val context = element.findContextElement()
            if (context.isValid) {
                return resolver.resolveReference(context)
            }
            return ResolveResult.EMPTY_ARRAY
        }

        override fun getVariants(): Array<Any> {
            val context = element.findContextElement()
            if (context.isValid) {
                return resolver.collectVariants(element.findContextElement())
            }
            return ArrayUtil.EMPTY_OBJECT_ARRAY
        }
    }
}

private fun PsiElement.findContextElement(): PsiElement {
    var current: PsiElement
    var parent = this

    do {
        current = parent
        parent = current.parent
        if (parent is PsiNameValuePair || parent is PsiArrayInitializerMemberValue) {
            return current
        }
    } while (parent is PsiExpression)

    throw IllegalStateException("Cannot find context element of $this")
}

/**
 * Patches the [LookupElementBuilder] to replace the element with a single
 * [PsiLiteral] when using code completion.
 */
fun LookupElementBuilder.completeToLiteral(
    context: PsiElement,
    extraAction: ((Editor, PsiLiteral) -> Unit)? = null
): LookupElementBuilder {
    if (context is PsiLiteral && extraAction == null) {
        // Context is already a literal
        return this
    }

    // TODO: Currently we replace everything with a single PsiLiteral,
    // not sure how you would keep line breaks after completion
    return withInsertHandler { insertionContext, item ->
        insertionContext.laterRunnable =
            ReplaceElementWithLiteral(insertionContext.editor, insertionContext.file, item.lookupString, extraAction)
    }
}

private class ReplaceElementWithLiteral(
    private val editor: Editor,
    private val file: PsiFile,
    private val text: String,
    private val extraAction: ((Editor, PsiLiteral) -> Unit)?
) : Runnable {

    override fun run() {
        // Commit changes made by code completion
        PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)

        // Run command to replace PsiElement
        CommandProcessor.getInstance().runUndoTransparentAction {
            runWriteAction {
                val element = file.findElementAt(editor.caretModel.offset)!!.findContextElement()
                val newElement = element.replace(
                    JavaPsiFacade.getElementFactory(element.project).createExpressionFromText(
                        "\"$text\"",
                        element.parent,
                    ),
                ) as PsiLiteral
                val extraAction = this.extraAction
                if (extraAction != null) {
                    extraAction(editor, newElement)
                }
            }
        }
    }
}
