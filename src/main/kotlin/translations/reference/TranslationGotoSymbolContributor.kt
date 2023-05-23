/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.translations.reference

import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.index.TranslationInverseIndex
import com.demonwav.mcdev.translations.index.merge
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex

class TranslationGotoSymbolContributor : ChooseByNameContributor {
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        val scope = if (includeNonProjectItems) {
            GlobalSearchScope.allScope(project)
        } else {
            GlobalSearchScope.projectScope(project)
        }
        val keys = FileBasedIndex.getInstance().getAllKeys(TranslationIndex.NAME, project)
        val translations = keys
            .asSequence()
            .flatMap { TranslationIndex.getEntries(scope, it).merge("").translations.asSequence() }

        return translations.map { it.key }.distinct().filter { it.isNotEmpty() }.toTypedArray()
    }

    override fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean,
    ): Array<NavigationItem> {
        val scope = if (includeNonProjectItems) {
            GlobalSearchScope.allScope(project)
        } else {
            GlobalSearchScope.projectScope(project)
        }
        val elements = TranslationInverseIndex.findElements(name, scope)

        return elements.mapToArray { it as NavigationItem }
    }
}
