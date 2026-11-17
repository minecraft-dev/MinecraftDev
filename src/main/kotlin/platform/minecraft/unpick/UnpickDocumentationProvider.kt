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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement

class UnpickDocumentationProvider : AbstractDocumentationProvider() {

    override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement?): String? {
        val definition = element as? UnpickGroupDefinition ?: return null
        return StringUtil.escapeXmlEntities(signature(definition))
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val definition = element as? UnpickGroupDefinition ?: return null
        val definitions = definition.name?.let { UnpickGroups.findDefinitions(definition, it) } ?: listOf(definition)
        val docs = definitions.mapNotNull { it.docComment }.ifEmpty { listOfNotNull(definition.docComment) }.distinct()

        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append(StringUtil.escapeXmlEntities(signature(definition)))
            append(DocumentationMarkup.DEFINITION_END)
            if (docs.isNotEmpty()) {
                append(DocumentationMarkup.CONTENT_START)
                append(docs.joinToString("<p>") { StringUtil.escapeXmlEntities(it).replace("\n", "<br>") })
                append(DocumentationMarkup.CONTENT_END)
            }
        }
    }

    private fun signature(definition: UnpickGroupDefinition): String {
        return listOfNotNull("group", definition.dataType?.text, definition.name).joinToString(" ")
    }
}
