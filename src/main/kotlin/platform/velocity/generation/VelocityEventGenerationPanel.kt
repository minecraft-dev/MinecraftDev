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

package com.demonwav.mcdev.platform.velocity.generation

import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.intellij.psi.PsiClass
import javax.swing.JComboBox
import javax.swing.JPanel

class VelocityEventGenerationPanel(chosenClass: PsiClass) : EventGenerationPanel(chosenClass) {

    private lateinit var parentPanel: JPanel
    private lateinit var eventOrderComboBox: JComboBox<String>

    override val panel: JPanel
        get() {
            // Not static because the form builder is not reliable
            eventOrderComboBox.addItem("FIRST")
            eventOrderComboBox.addItem("EARLY")
            eventOrderComboBox.addItem("NORMAL")
            eventOrderComboBox.addItem("LATE")
            eventOrderComboBox.addItem("LAST")

            eventOrderComboBox.selectedIndex = 2

            return parentPanel
        }

    override fun gatherData(): GenerationData {
        return VelocityGenerationData(eventOrderComboBox.selectedItem as String)
    }
}
