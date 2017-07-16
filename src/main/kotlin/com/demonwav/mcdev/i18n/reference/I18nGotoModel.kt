/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.reference

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
import com.google.common.collect.Lists
import com.intellij.ide.util.gotoByName.ContributorsBasedGotoByModel
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.indexing.FindSymbolParameters
import java.util.TreeSet

class I18nGotoModel(project: Project, val filter: Regex? = null) : ContributorsBasedGotoByModel(project, arrayOf(Extensions.findExtension(ChooseByNameContributor.SYMBOL_EP_NAME, I18nGotoSymbolContributor::class.java))) {
    override fun acceptItem(item: NavigationItem?): Boolean {
        return (item as I18nProperty).containingFile.virtualFile.nameWithoutExtension.toLowerCase() == "en_us"
    }

    override fun getElementsByName(name: String, parameters: FindSymbolParameters, canceled: ProgressIndicator): Array<Any> {
        val superResult = Lists.newArrayList(*super.getElementsByName(name, parameters, canceled))
        val result = TreeSet<Any> { o1, o2 -> (o1 as I18nProperty).key.compareTo((o2 as I18nProperty).key) }
        if (filter != null)
            result.addAll(superResult.filter { filter.matches((it as I18nProperty).key) })
        else
            result.addAll(superResult)
        return result.toArray()
    }

    override fun getPromptText() = "Choose translation to use"

    override fun getNotInMessage() = "test"

    override fun getNotFoundMessage() = "test"

    override fun getCheckBoxName() = "Include &&non-project translations"

    override fun getCheckBoxMnemonic() = 0.toChar()

    override fun loadInitialCheckBoxState() = false

    override fun saveInitialCheckBoxState(state: Boolean) {
    }

    override fun getSeparators(): Array<String> = emptyArray()

    override fun getFullName(element: Any) = element.toString()

    override fun willOpenEditor() = false
}
