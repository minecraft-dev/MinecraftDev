/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.toml.platform.forge

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProviderEx
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.toml.lang.psi.TomlHeaderOwner
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlTableHeader

class ModsTomlDocumentationProvider : DocumentationProviderEx() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?
    ): PsiElement? {
        if (!isModsToml(contextElement)) {
            return null
        }
        // If this is a header get the first key as TomlSchema only remembers the first one
        return contextElement?.parentOfType<TomlTableHeader>()?.key?.segments?.firstOrNull()
            ?: contextElement?.parentOfType<TomlKeySegment>()
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        if (element !is TomlKeySegment || !isModsToml(originalElement)) {
            return null
        }

        val key = element.parentOfType<TomlKey>() ?: return null
        val schema = ModsTomlSchema.get(element.project)
        val table = element.parentOfType<TomlKeyValueOwner>()
        val description = when (val parent = key.parent) {
            is TomlTableHeader -> {
                if (element != parent.key?.segments?.firstOrNull()) {
                    return null
                }
                schema.tableSchema(element.text)?.description
            }
            is TomlKeyValue -> when (table) {
                is TomlHeaderOwner -> {
                    val tableName = table.header.key?.segments?.firstOrNull()?.text ?: return null
                    schema.tableEntry(tableName, key.text)?.description
                }
                null -> schema.topLevelEntries.find { it.key == key.text }?.description
                else -> null
            }
            else -> null
        }?.takeUnless { it.isEmpty() } ?: return null
        return DocumentationMarkup.CONTENT_START + description.joinToString("<br>") + DocumentationMarkup.CONTENT_END
    }

    private fun isModsToml(element: PsiElement?): Boolean =
        element?.containingFile?.virtualFile?.name == ForgeConstants.MODS_TOML
}
