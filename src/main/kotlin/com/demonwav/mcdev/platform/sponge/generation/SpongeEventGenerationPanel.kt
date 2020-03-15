/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.generation

import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.intellij.psi.PsiClass
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel

class SpongeEventGenerationPanel(chosenClass: PsiClass) : EventGenerationPanel(chosenClass) {

    private lateinit var parentPanel: JPanel
    private lateinit var eventOrderComboBox: JComboBox<String>
    private lateinit var ignoreCanceledCheckBox: JCheckBox

    override val panel: JPanel?
        get() {
            ignoreCanceledCheckBox.isSelected = true

            // Not static because the form builder is not reliable
            eventOrderComboBox.addItem("PRE")
            eventOrderComboBox.addItem("AFTER_PRE")
            eventOrderComboBox.addItem("FIRST")
            eventOrderComboBox.addItem("EARLY")
            eventOrderComboBox.addItem("DEFAULT")
            eventOrderComboBox.addItem("LATE")
            eventOrderComboBox.addItem("LAST")
            eventOrderComboBox.addItem("BEFORE_POST")
            eventOrderComboBox.addItem("POST")

            eventOrderComboBox.selectedIndex = 4

            return parentPanel
        }

    override fun gatherData(): GenerationData? {
        return SpongeGenerationData(ignoreCanceledCheckBox.isSelected, eventOrderComboBox.selectedItem as String)
    }
}
