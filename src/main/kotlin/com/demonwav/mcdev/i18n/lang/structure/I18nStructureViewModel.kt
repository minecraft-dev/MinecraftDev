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

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.psi.PsiFile

class I18nStructureViewModel(psiFile: PsiFile) :
    StructureViewModelBase(psiFile, I18nStructureViewElement(psiFile)), StructureViewModel.ElementInfoProvider {
    override fun getSorters() = arrayOf(Sorter.ALPHA_SORTER)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement) = false
}
