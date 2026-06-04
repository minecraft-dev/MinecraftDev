/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.util.psiType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.impl.TypeExpression
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.codeInsight.template.impl.Variable
import com.intellij.psi.PsiClass
import com.jetbrains.rd.util.string.printToString

class BukkitEventHandlerInsertHandler(
    private val psiClass: PsiClass
) : InsertHandler<LookupElement> {

    override fun handleInsert(insertionContext: InsertionContext, lookupElement: LookupElement) {
        insertionContext.document.deleteString(
            insertionContext.startOffset,
            insertionContext.tailOffset
        )

        val sourceTemplate = TemplateSettings.getInstance().getTemplate("event_handler", "Bukkit") ?: return
        val template = sourceTemplate.copy()

        val classTypeExpr = TypeExpression(insertionContext.project, listOf(psiClass.psiType))

        val index = template.variables.indexOfFirst { it.name == "EVENT_CLASS" }
        if (index != -1) {
            template.removeVariable(index)
        }

        template.addVariable(Variable("EVENT_CLASS", classTypeExpr, classTypeExpr, false, false));

        TemplateManager.getInstance(insertionContext.project).startTemplate(insertionContext.editor, template)
    }
}
