/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
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

        override fun resolve() = resolver.resolveReference(element.findContextElement())
        override fun getVariants() = resolver.collectVariants(element.findContextElement())
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

        override fun multiResolve(incompleteCode: Boolean) = resolver.resolveReference(element.findContextElement())
        override fun getVariants() = resolver.collectVariants(element.findContextElement())
    }
}

private fun PsiElement.findContextElement(): PsiElement {
    var current: PsiElement
    var parent = this

    do {
        current = parent
        parent = current.parent
        if (parent is PsiNameValuePair) {
            return current
        }
    } while (parent is PsiExpression)

    throw IllegalStateException("Cannot find context element of $this")
}

/**
 * Patches the [LookupElementBuilder] to replace the element with a single
 * [PsiLiteral] when using code completion.
 */
fun LookupElementBuilder.completeToLiteral(context: PsiElement): LookupElementBuilder {
    if (context is PsiLiteral) {
        // Context is already a literal
        return this
    }

    // TODO: Currently we replace everything with a single PsiLiteral,
    // not sure how you would keep line breaks after completion
    return withInsertHandler { insertionContext, item ->
        insertionContext.laterRunnable =
            ReplaceElementWithLiteral(insertionContext.editor, insertionContext.file, item.lookupString)
    }
}

private class ReplaceElementWithLiteral(
    private val editor: Editor,
    private val file: PsiFile,
    private val text: String
) : Runnable {

    override fun run() {
        // Commit changes made by code completion
        PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)

        // Run command to replace PsiElement
        CommandProcessor.getInstance().runUndoTransparentAction {
            runWriteAction {
                val element = file.findElementAt(editor.caretModel.offset)!!.findContextElement()
                element.replace(
                    JavaPsiFacade.getElementFactory(element.project).createExpressionFromText(
                        "\"$text\"",
                        element.parent
                    )
                )
            }
        }
    }
}
