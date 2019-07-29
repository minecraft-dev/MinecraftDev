/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.util.toArray
import com.intellij.ide.util.PsiClassListCellRenderer
import com.intellij.psi.PsiClass
import com.intellij.ui.components.JBList
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.ListModel

class FindMixinsComponent(classes: List<PsiClass>) : MouseAdapter() {

    private lateinit var classList: JBList<PsiClass>
    lateinit var panel: JPanel
        private set

    init {
        @Suppress("UNCHECKED_CAST")
        classList.model = JBList.createDefaultListModel(*classes.toArray()) as ListModel<PsiClass>
        classList.cellRenderer = PsiClassListCellRenderer()

        classList.addMouseListener(this)
    }

    override fun mouseClicked(e: MouseEvent) {
        classList.selectedValue?.takeIf(PsiClass::canNavigate)?.navigate(true)
    }
}
