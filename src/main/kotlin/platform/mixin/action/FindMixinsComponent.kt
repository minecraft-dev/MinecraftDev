/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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
