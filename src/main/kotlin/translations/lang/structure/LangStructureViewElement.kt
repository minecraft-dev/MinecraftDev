/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang.structure

import com.demonwav.mcdev.translations.lang.LangFile
import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.demonwav.mcdev.util.mapToArray
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil

class LangStructureViewElement(private val element: PsiElement) : StructureViewTreeElement, SortableTreeElement {
    override fun getValue() = element

    override fun navigate(requestFocus: Boolean) {
        if (element is NavigationItem) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate() = element is NavigationItem && element.canNavigate()

    override fun canNavigateToSource() = element is NavigationItem && element.canNavigateToSource()

    override fun getAlphaSortKey() = (element as PsiNamedElement).name!!

    override fun getPresentation() = (element as NavigationItem).presentation!!

    override fun getChildren() =
        when (element) {
            is LangFile -> {
                val entries = PsiTreeUtil.getChildrenOfType(element, LangEntry::class.java) ?: emptyArray()
                entries.mapToArray(::LangStructureViewElement)
            }
            else -> emptyArray()
        }
}
