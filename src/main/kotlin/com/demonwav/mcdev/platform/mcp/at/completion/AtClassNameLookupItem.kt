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
import com.intellij.util.PlatformIcons

data class AtClassNameLookupItem(private val className: AtClassName) : LookupItem<AtClassName>(className, className.text) {

    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.icon = if (priority == 0.0) PlatformIcons.PACKAGE_ICON else PlatformIcons.CLASS_ICON
        presentation.itemText = className.text
    }

    override fun handleInsert(context: InsertionContext) {
        val currentElement = context.file.findElementAt(context.startOffset) ?: return
        currentElement.replace(className)
    }
}
