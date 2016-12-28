/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.util.findParent
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable

internal abstract class ConstantLiteralReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element), MixinReference {

    val context = findContextElement(element)

    override fun getValue(): String = context.constantValue

    protected fun patchLookup(lookup: LookupElementBuilder): LookupElementBuilder = patchLookup(lookup, context)

    abstract class Poly(element: PsiElement) : PsiReferenceBase.Poly<PsiElement>(element), MixinReference.Poly {

        val context = findContextElement(element)

        override fun getValue() = context.constantValue

        protected fun patchLookup(lookup: LookupElementBuilder): LookupElementBuilder = patchLookup(lookup, context)

    }

}

private fun findContextElement(element: PsiElement): PsiElement {
    return findParent<PsiNameValuePair>(element)!!.value!!
}

private fun patchLookup(lookup: LookupElementBuilder, context: PsiElement): LookupElementBuilder {
    if (context is PsiLiteral) {
        // Can insert normally
        return lookup
    }

    // TODO: Currently we replace everything with a single PsiLiteral,
    // not sure how you would keep line breaks after completion
    return lookup
            .withInsertHandler({ context, item ->
                context.laterRunnable = ReplaceElementWithLiteral(context.editor, context.file, item.lookupString)
            })
}

private class ReplaceElementWithLiteral(val editor: Editor, val file: PsiFile, val text: String) : Runnable {

    override fun run() {
        // Commit changes made by code completion
        PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)

        // Run command to replace PsiElement
        CommandProcessor.getInstance().runUndoTransparentAction {
            runWriteAction {
                val element = findContextElement(file.findElementAt(editor.caretModel.offset)!!)
                element.replace(JavaPsiFacade.getElementFactory(element.project).createExpressionFromText("\"$text\"", element.parent))
            }
        }
    }

}

internal val PsiElement.constantValue: String
    get() = when (this) {
        is PsiLiteral -> value?.toString() ?: ""
        is PsiPolyadicExpression ->
            // We assume that the expression uses the '+' operator since that is the only valid one for constant expressions (of strings)
            operands.joinToString(separator = "", transform = PsiElement::constantValue)
        is PsiReferenceExpression ->
            // Possibly a reference to a constant field, attempt to resolve
            (resolve() as? PsiVariable)?.computeConstantValue()?.toString() ?: ""
        is PsiParenthesizedExpression ->
            // Useless parentheses? Fine with me!
            expression?.constantValue ?: ""
        is PsiTypeCastExpression ->
            // Useless type cast? Pfff.
            operand?.constantValue ?: ""
        else -> throw UnsupportedOperationException("Unsupported expression type: $this")
    }
