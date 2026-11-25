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
import com.demonwav.mcdev.platform.minecraft.unpick.index.UnpickGroupIndex
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex

object UnpickGroups {

    /**
     * Finds all definitions of the group with the given name, or of the default group if [name] is `null`.
     */
    fun findDefinitions(context: PsiElement, name: String?): List<UnpickGroupDefinition> {
        return findFiles(context, name ?: UnpickGroupIndex.Util.DEFAULT_GROUP_KEY).flatMap { file ->
            file.groupDefinitions.filter { it.name == name }
        }
    }

    /**
     * Returns all group names visible from the given element, excluding the default group.
     */
    fun findAllGroupNames(context: PsiElement): Set<String> {
        val project = context.project
        if (DumbService.isDumb(project)) {
            return (context.containingFile as? UnpickFile)?.groupDefinitions?.mapNotNullTo(HashSet()) { it.name }
                ?: emptySet()
        }
        val scope = searchScope(context)
        val index = FileBasedIndex.getInstance()
        val result = HashSet<String>()
        index.processAllKeys(
            UnpickGroupIndex.Util.NAME,
            { key ->
                if (key != UnpickGroupIndex.Util.DEFAULT_GROUP_KEY && key !in result) {
                    if (index.getContainingFiles(UnpickGroupIndex.Util.NAME, key, scope).isNotEmpty()) {
                        result += key
                    }
                }
                true
            },
            scope,
            null,
        )
        (context.containingFile as? UnpickFile)?.groupDefinitions?.mapNotNullTo(result) { it.name }
        return result
    }

    private fun findFiles(context: PsiElement, key: String): List<UnpickFile> {
        val project = context.project
        val ownFile = context.containingFile?.originalFile as? UnpickFile
        if (DumbService.isDumb(project)) {
            return listOfNotNull(ownFile)
        }
        val psiManager = PsiManager.getInstance(project)
        val files = FileBasedIndex.getInstance()
            .getContainingFiles(UnpickGroupIndex.Util.NAME, key, searchScope(context))
        val result = files.mapNotNull { psiManager.findFile(it) as? UnpickFile }.toMutableList()
        if (ownFile != null && ownFile !in result) {
            result += ownFile
        }
        return result
    }

    private fun searchScope(context: PsiElement): GlobalSearchScope {
        return context.containingFile?.originalFile?.resolveScope ?: GlobalSearchScope.allScope(context.project)
    }
}
