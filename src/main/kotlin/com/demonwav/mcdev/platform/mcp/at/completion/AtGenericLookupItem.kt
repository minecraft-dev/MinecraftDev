
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

import com.demonwav.mcdev.platform.mcp.at.psi.AtElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupItem
import javax.swing.Icon

class AtGenericLookupItem<T : AtElement>(private val element: T, private val icon: Icon? = null) : LookupItem<T>(element, element.text) {

    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.icon = icon
        presentation.itemText = element.text
    }

    override fun handleInsert(context: InsertionContext) {
        val currentElement = context.file.findElementAt(context.startOffset) ?: return
        currentElement.replace(element)
    }
}
