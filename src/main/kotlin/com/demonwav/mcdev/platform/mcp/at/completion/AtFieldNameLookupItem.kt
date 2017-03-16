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
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupItem
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.PlatformIcons

class AtFieldNameLookupItem(private val fieldName: AtFieldName, private val prettyText: String) : LookupItem<AtFieldName>(fieldName, fieldName.text), AtMcpLookupItem {

    init {
        priority = 1.0
    }

    override fun getPrettyText() = prettyText

    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.icon = PlatformIcons.FIELD_ICON
        presentation.itemText = prettyText
    }

    override fun handleInsert(context: InsertionContext) {
        val currentElement = context.file.findElementAt(context.startOffset) ?: return
        currentElement.replace(fieldName)

        // TODO: Fix visibility decreases
        PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(context.document)
        val comment = " # $prettyText"
        context.document.insertString(context.editor.caretModel.offset, comment)
        context.editor.caretModel.moveCaretRelatively(comment.length, 0, false, false, false)
    }
}
