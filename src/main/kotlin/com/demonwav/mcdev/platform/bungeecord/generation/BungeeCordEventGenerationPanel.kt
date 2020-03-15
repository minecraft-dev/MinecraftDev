/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.generation

import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.intellij.psi.PsiClass
import javax.swing.JComboBox
import javax.swing.JPanel

class BungeeCordEventGenerationPanel(chosenClass: PsiClass) : EventGenerationPanel(chosenClass) {

    private lateinit var eventPriorityComboBox: JComboBox<String>
    private lateinit var parentPanel: JPanel

    override val panel: JPanel?
        get() {
            // Not static because the form builder is not reliable
            eventPriorityComboBox.addItem("HIGHEST")
            eventPriorityComboBox.addItem("HIGH")
            eventPriorityComboBox.addItem("NORMAL")
            eventPriorityComboBox.addItem("LOW")
            eventPriorityComboBox.addItem("LOWEST")

            eventPriorityComboBox.selectedIndex = 2

            return parentPanel
        }

    override fun gatherData() = BungeeCordGenerationData(eventPriorityComboBox.selectedItem.toString())
}
