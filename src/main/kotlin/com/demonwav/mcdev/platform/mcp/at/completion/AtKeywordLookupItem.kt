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

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtKeyword
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupItem

class AtKeywordLookupItem(private val keyword: AtKeyword) : LookupItem<AtKeyword>(keyword, keyword.text) {

    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.itemText = keyword.text
    }

    override fun handleInsert(context: InsertionContext) {
        val currentElement = context.file.findElementAt(context.startOffset) ?: return
        currentElement.replace(keyword)
    }
}
