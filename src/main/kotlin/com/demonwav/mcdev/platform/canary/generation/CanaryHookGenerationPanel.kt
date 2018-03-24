/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.generation

import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.intellij.psi.PsiClass
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JRadioButton

class CanaryHookGenerationPanel(chosenClass: PsiClass) : EventGenerationPanel(chosenClass) {

    private lateinit var ignoreCanceledRadioButton: JRadioButton
    private lateinit var parentPanel: JPanel
    private lateinit var hookPriorityComboBox: JComboBox<String>

    override val panel: JPanel?
        get() {
            ignoreCanceledRadioButton.isSelected = true

            // Not static because the form builder is not reliable
            hookPriorityComboBox.addItem("PASSIVE")
            hookPriorityComboBox.addItem("LOW")
            hookPriorityComboBox.addItem("NORMAL")
            hookPriorityComboBox.addItem("HIGH")
            hookPriorityComboBox.addItem("CRITICAL")

            hookPriorityComboBox.selectedIndex = 3

            return parentPanel
        }

    override fun gatherData() = CanaryGenerationData(ignoreCanceledRadioButton.isSelected, hookPriorityComboBox.selectedItem.toString())
}
