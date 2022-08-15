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

import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.translations.TranslationFiles
import com.intellij.ide.util.gotoByName.ContributorsBasedGotoByModel
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.util.indexing.FindSymbolParameters
import java.util.TreeSet

class TranslationGotoModel(project: Project, private val prefix: String, private val suffix: String) :
    ContributorsBasedGotoByModel(
        project,
        arrayOf(
            ChooseByNameContributor.SYMBOL_EP_NAME.findExtensionOrFail(TranslationGotoSymbolContributor::class.java)
        )
    ) {
    override fun acceptItem(item: NavigationItem?): Boolean {
        return TranslationFiles.getLocale(
            (item as PsiElement).containingFile?.virtualFile
        ) == TranslationConstants.DEFAULT_LOCALE
    }

    override fun getElementsByName(
        name: String,
        parameters: FindSymbolParameters,
        canceled: ProgressIndicator
    ): Array<Any> {
        val superResult = super.getElementsByName(name, parameters, canceled).asSequence()
        val result = TreeSet<PsiNamedElement> { o1, o2 ->
            (o1 as PsiNamedElement).name?.compareTo((o2 as PsiNamedElement).name ?: return@TreeSet -1) ?: -1
        }
        result.addAll(
            superResult.map { it as PsiNamedElement }.filter {
                val key = it.name ?: return@filter false
                key.startsWith(prefix) && key.endsWith(suffix)
            }
        )
        return result.toArray()
    }

    override fun getPromptText() = "Choose translation to use"

    override fun getNotInMessage() = "Couldn't find translation with that name"

    override fun getNotFoundMessage() = "Couldn't find translation with that name"

    override fun getCheckBoxName() = "Include non-project translations"

    override fun loadInitialCheckBoxState() = false

    override fun saveInitialCheckBoxState(state: Boolean) {
    }

    override fun getSeparators(): Array<String> = emptyArray()

    override fun getFullName(element: Any) = element.toString()

    override fun willOpenEditor() = false
}
