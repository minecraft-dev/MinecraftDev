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
