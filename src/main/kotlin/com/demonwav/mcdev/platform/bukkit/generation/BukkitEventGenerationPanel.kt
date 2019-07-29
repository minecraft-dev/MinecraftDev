/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.generation

import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.intellij.psi.PsiClass
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel

class BukkitEventGenerationPanel(chosenClass: PsiClass) : EventGenerationPanel(chosenClass) {

    private lateinit var ignoreCanceledCheckBox: JCheckBox
    private lateinit var parentPanel: JPanel
    private lateinit var eventPriorityComboBox: JComboBox<String>

    override val panel: JPanel?
        get() {
            ignoreCanceledCheckBox.isSelected = true

            // Not static because the form builder is not reliable
            eventPriorityComboBox.addItem("MONITOR")
            eventPriorityComboBox.addItem("HIGHEST")
            eventPriorityComboBox.addItem("HIGH")
            eventPriorityComboBox.addItem("NORMAL")
            eventPriorityComboBox.addItem("LOW")
            eventPriorityComboBox.addItem("LOWEST")

            eventPriorityComboBox.selectedIndex = 3

            return parentPanel
        }

    override fun gatherData(): GenerationData? {
        return BukkitGenerationData(ignoreCanceledCheckBox.isSelected, eventPriorityComboBox.selectedItem.toString())
    }
}
