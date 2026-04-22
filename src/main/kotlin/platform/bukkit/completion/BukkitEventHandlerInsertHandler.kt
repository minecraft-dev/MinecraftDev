/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.bukkit.completion

import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.NotNull

class BukkitEventHandlerInsertHandler(
    private val methodName: String,
    private val qualifiedName: @NlsSafe String?
) : InsertHandler<LookupElement> {

    override fun handleInsert(insertionContext: InsertionContext, lookupElement: LookupElement) {
        val project = insertionContext.project
        val editor = insertionContext.editor

        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("", "")
        template.isToReformat = true

        template.addTextSegment("@${BukkitConstants.HANDLER_ANNOTATION}\n")
        template.addTextSegment("public void ")
        template.addVariable("METHOD_NAME", TextExpression(methodName), true)
        template.addTextSegment("($qualifiedName ")
        template.addVariable("EVENT_PARAM", TextExpression("event"), true)
        template.addTextSegment(") { \n")
        template.addEndVariable()
        template.addTextSegment("\n}")

        insertionContext.document.deleteString(
            insertionContext.startOffset,
            insertionContext.tailOffset
        )

        templateManager.startTemplate(editor, template)

    }
}
