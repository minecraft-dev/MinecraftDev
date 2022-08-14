/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
        includeNonProjectItems: Boolean
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
