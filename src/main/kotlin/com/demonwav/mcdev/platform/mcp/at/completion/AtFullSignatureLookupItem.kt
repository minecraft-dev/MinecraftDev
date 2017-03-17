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

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtClassName
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupItem
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.PlatformIcons

class AtFullSignatureLookupItem(private val className: AtClassName, private val fieldText: String, private val prettyText: String) : LookupItem<AtClassName>(className, fieldText), AtMcpLookupItem {

    init {
        priority = 10_000_000.0
    }

    override fun getPrettyText() = prettyText

    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.icon = PlatformIcons.FIELD_ICON
        presentation.itemText = prettyText
        presentation.setTailText(" (${className.text} $fieldText)", true)
    }

    override fun handleInsert(context: InsertionContext) {
        val currentElement = context.file.findElementAt(context.startOffset) ?: return
        currentElement.replace(className)

        // TODO: Fix visibility decreases
        val comment = " $fieldText # $prettyText"
        PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(context.document)
        context.document.insertString(context.editor.caretModel.offset, comment)
        context.editor.caretModel.moveCaretRelatively(comment.length, 0, false, false, false)
    }
}
