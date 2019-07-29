/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.reference

import com.demonwav.mcdev.i18n.Scope
import com.demonwav.mcdev.i18n.findLangEntries
import com.demonwav.mcdev.util.mapToArray
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project

class I18nGotoSymbolContributor : ChooseByNameContributor {
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        val entries = if (includeNonProjectItems) project.findLangEntries() else project.findLangEntries(Scope.PROJECT)
        return entries.filter { it.key.isNotEmpty() }.mapToArray { it.key }
    }

    override fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean
    ): Array<NavigationItem> {
        val entries = if (includeNonProjectItems) project.findLangEntries(key = name) else project.findLangEntries(
            Scope.PROJECT,
            name
        )
        return entries.mapToArray { it as NavigationItem }
    }
}
