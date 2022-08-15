/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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

    override val panel: JPanel?
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

    override fun gatherData(): GenerationData? {
        return VelocityGenerationData(eventOrderComboBox.selectedItem as String)
    }
}
