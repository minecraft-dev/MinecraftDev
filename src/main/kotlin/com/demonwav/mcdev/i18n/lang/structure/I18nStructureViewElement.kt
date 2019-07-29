/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.structure

import com.demonwav.mcdev.i18n.lang.I18nFile
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.util.mapToArray
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil

class I18nStructureViewElement(private val element: PsiElement) : StructureViewTreeElement, SortableTreeElement {
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
            is I18nFile -> {
                val entries = PsiTreeUtil.getChildrenOfType(element, I18nEntry::class.java) ?: emptyArray()
                entries.mapToArray(::I18nStructureViewElement)
            }
            else -> emptyArray()
        }
}
