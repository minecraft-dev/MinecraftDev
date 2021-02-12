/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.toml.platform.forge

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.toml.lang.psi.TomlHeaderOwner
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlTableHeader

class ModsTomlDocumentationProvider : DocumentationProvider {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (!isModsToml(contextElement)) {
            return null
        }
        // If this is a header get the first key as TomlSchema only remembers the first one
        return contextElement?.parentOfType<TomlTableHeader>()?.names?.firstOrNull()
            ?: contextElement?.parentOfType<TomlKey>()
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        if (element !is TomlKey || !isModsToml(originalElement)) {
            return null
        }

        val key = element.text
        val schema = ModsTomlSchema.get(element.project)
        val table = element.parentOfType<TomlKeyValueOwner>()
        val description = when (val parent = element.parent) {
            is TomlTableHeader -> {
                if (element != parent.names.firstOrNull()) {
                    return null
                }
                schema.tableSchema(key)?.description
            }
            is TomlKeyValue -> when (table) {
                is TomlHeaderOwner -> {
                    val tableName = table.header.names.firstOrNull()?.text ?: return null
                    schema.tableEntry(tableName, key)?.description
                }
                null -> schema.topLevelEntries.find { it.key == key }?.description
                else -> null
            }
            else -> null
        }?.takeUnless { it.isEmpty() } ?: return null
        return DocumentationMarkup.CONTENT_START + description.joinToString("<br>") + DocumentationMarkup.CONTENT_END
    }

    private fun isModsToml(element: PsiElement?): Boolean =
        element?.containingFile?.virtualFile?.name == ForgeConstants.MODS_TOML
}
