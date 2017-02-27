/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.util.ui.UIUtil
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class MinecraftFacetEditorTab(private val configuration: MinecraftFacetConfiguration) : FacetEditorTab() {

    private lateinit var panel: JPanel

    private lateinit var bukkitCheckBox: JCheckBox
    private lateinit var spigotCheckBox: JCheckBox
    private lateinit var paperCheckBox: JCheckBox
    private lateinit var spongeCheckBox: JCheckBox
    private lateinit var forgeCheckBox: JCheckBox
    private lateinit var liteloaderCheckBox: JCheckBox
    private lateinit var mcpCheckBox: JCheckBox
    private lateinit var mixinsCheckBox: JCheckBox
    private lateinit var bungeecordCheckBox: JCheckBox
    private lateinit var neptuneCheckBox: JCheckBox
    private lateinit var canaryCheckBox: JCheckBox

    private lateinit var spongeIcon: JLabel
    private lateinit var mcpIcon: JLabel
    private lateinit var mixinIcon: JLabel

    override fun createComponent(): JComponent {
        if (UIUtil.isUnderDarcula()) {
            spongeIcon.icon = PlatformAssets.SPONGE_ICON_2X_DARK
            mcpIcon.icon = PlatformAssets.MCP_ICON_2X_DARK
            mixinIcon.icon = PlatformAssets.MIXIN_ICON_2X_DARK
        }

        bukkitCheckBox.addActionListener { unique(bukkitCheckBox, spigotCheckBox, paperCheckBox) }
        spigotCheckBox.addActionListener { unique(spigotCheckBox, bukkitCheckBox, paperCheckBox) }
        paperCheckBox.addActionListener { unique(paperCheckBox, bukkitCheckBox, spigotCheckBox) }

        canaryCheckBox.addActionListener { unique(canaryCheckBox, neptuneCheckBox) }
        neptuneCheckBox.addActionListener { unique(neptuneCheckBox, canaryCheckBox) }

        forgeCheckBox.addActionListener { also(forgeCheckBox, mcpCheckBox) }
        liteloaderCheckBox.addActionListener { also(liteloaderCheckBox, mcpCheckBox) }
        mixinsCheckBox.addActionListener { also(mixinsCheckBox, mcpCheckBox) }

        return panel
    }

    override fun getDisplayName() = "Minecraft Module Settings"

    override fun isModified(): Boolean {
        val state = configuration.state
        return bukkitCheckBox.isSelected != state.types.contains(PlatformType.BUKKIT) ||
            spigotCheckBox.isSelected != state.types.contains(PlatformType.SPIGOT) ||
            paperCheckBox.isSelected != state.types.contains(PlatformType.PAPER) ||
            spongeCheckBox.isSelected != state.types.contains(PlatformType.SPONGE) ||
            forgeCheckBox.isSelected != state.types.contains(PlatformType.FORGE) ||
            liteloaderCheckBox.isSelected != state.types.contains(PlatformType.LITELOADER) ||
            mcpCheckBox.isSelected != state.types.contains(PlatformType.MCP) ||
            mixinsCheckBox.isSelected != state.types.contains(PlatformType.MIXIN) ||
            bungeecordCheckBox.isSelected != state.types.contains(PlatformType.BUNGEECORD) ||
            neptuneCheckBox.isSelected != state.types.contains(PlatformType.NEPTUNE) ||
            canaryCheckBox.isSelected != state.types.contains(PlatformType.CANARY)
    }

    override fun reset() {
        val state = configuration.state
        bukkitCheckBox.isSelected = state.types.contains(PlatformType.BUKKIT) == true
        spigotCheckBox.isSelected = state.types.contains(PlatformType.SPIGOT) == true
        paperCheckBox.isSelected = state.types.contains(PlatformType.PAPER) == true
        spongeCheckBox.isSelected = state.types.contains(PlatformType.SPONGE) == true
        forgeCheckBox.isSelected = state.types.contains(PlatformType.FORGE) == true
        liteloaderCheckBox.isSelected = state.types.contains(PlatformType.LITELOADER) == true
        mcpCheckBox.isSelected = state.types.contains(PlatformType.MCP) == true
        mixinsCheckBox.isSelected = state.types.contains(PlatformType.MIXIN) == true
        bungeecordCheckBox.isSelected = state.types.contains(PlatformType.BUNGEECORD) == true
        neptuneCheckBox.isSelected = state.types.contains(PlatformType.NEPTUNE) == true
        canaryCheckBox.isSelected = state.types.contains(PlatformType.CANARY) == true
    }

    override fun apply() {
        val state = configuration.state
        state.types.clear()
        if (bukkitCheckBox.isSelected) {
            state.types.add(PlatformType.BUKKIT)
        }
        if (spigotCheckBox.isSelected) {
            state.types.add(PlatformType.SPIGOT)
        }
        if (paperCheckBox.isSelected) {
            state.types.add(PlatformType.PAPER)
        }
        if (spongeCheckBox.isSelected) {
            state.types.add(PlatformType.SPONGE)
        }
        if (forgeCheckBox.isSelected) {
            state.types.add(PlatformType.FORGE)
        }
        if (liteloaderCheckBox.isSelected) {
            state.types.add(PlatformType.LITELOADER)
        }
        if (mcpCheckBox.isSelected) {
            state.types.add(PlatformType.MCP)
        }
        if (mixinsCheckBox.isSelected) {
            state.types.add(PlatformType.MIXIN)
        }
        if (bungeecordCheckBox.isSelected) {
            state.types.add(PlatformType.BUNGEECORD)
        }
        if (neptuneCheckBox.isSelected) {
            state.types.add(PlatformType.NEPTUNE)
        }
        if (canaryCheckBox.isSelected) {
            state.types.add(PlatformType.CANARY)
        }
    }

    private fun unique(vararg checkBoxes: JCheckBox) {
        if (checkBoxes.size <= 1) {
            return
        }

        if (checkBoxes[0].isSelected) {
            for (i in 1 until checkBoxes.size) {
                checkBoxes[i].isSelected = false
            }
        }
    }

    private fun also(vararg checkBoxes: JCheckBox) {
        if (checkBoxes.size <= 1) {
            return
        }

        if (checkBoxes[0].isSelected) {
            for (i in 1 until checkBoxes.size) {
                checkBoxes[i].isSelected = true
            }
        }
    }
}
