/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.completion

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupItem
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.PlatformIcons

class AtFuncLookupItem(private val function: AtFunction, private val prettyText: String) : LookupItem<AtFunction>(function, function.text), AtMcpLookupItem {

    init {
        priority = 0.0
    }

    override fun getPrettyText() = prettyText

    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.icon = PlatformIcons.METHOD_ICON
        presentation.itemText = prettyText
        presentation.setTailText(" (${function.funcName.text})", true)
    }

    override fun handleInsert(context: InsertionContext) {
        var currentElement = context.file.findElementAt(context.startOffset) ?: return
        var counter = 0
        while (currentElement !is AtFieldName && currentElement !is AtFunction) {
            currentElement = currentElement.parent
            if (counter++ > 3) {
                break
            }
        }

        // Hopefully this won't happen lol
        if (currentElement !is AtFieldName && currentElement !is AtFunction) {
            return
        }

        currentElement.replace(function)

        // TODO: Fix visibility decreases
        PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(context.document)
        val comment = " # ${prettyText.substringBefore('(')}"
        context.document.insertString(context.editor.caretModel.offset, comment)
        context.editor.caretModel.moveCaretRelatively(comment.length, 0, false, false, false)
    }
}
