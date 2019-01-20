/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.util.ui.UIUtil
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class MinecraftFacetEditorTab(private val configuration: MinecraftFacetConfiguration) : FacetEditorTab() {

    private lateinit var panel: JPanel

    private lateinit var bukkitEnabledCheckBox: JCheckBox
    private lateinit var bukkitAutoCheckBox: JCheckBox
    private lateinit var spigotEnabledCheckBox: JCheckBox
    private lateinit var spigotAutoCheckBox: JCheckBox
    private lateinit var paperEnabledCheckBox: JCheckBox
    private lateinit var paperAutoCheckBox: JCheckBox
    private lateinit var spongeEnabledCheckBox: JCheckBox
    private lateinit var spongeAutoCheckBox: JCheckBox
    private lateinit var forgeEnabledCheckBox: JCheckBox
    private lateinit var forgeAutoCheckBox: JCheckBox
    private lateinit var liteloaderEnabledCheckBox: JCheckBox
    private lateinit var liteloaderAutoCheckBox: JCheckBox
    private lateinit var mcpEnabledCheckBox: JCheckBox
    private lateinit var mcpAutoCheckBox: JCheckBox
    private lateinit var mixinEnabledCheckBox: JCheckBox
    private lateinit var mixinAutoCheckBox: JCheckBox
    private lateinit var bungeecordEnabledCheckBox: JCheckBox
    private lateinit var bungeecordAutoCheckBox: JCheckBox
    private lateinit var waterfallEnabledCheckBox: JCheckBox
    private lateinit var waterfallAutoCheckBox: JCheckBox

    private lateinit var spongeIcon: JLabel
    private lateinit var mcpIcon: JLabel
    private lateinit var mixinIcon: JLabel

    private val enableCheckBoxArray: Array<JCheckBox> by lazy {
        arrayOf(
            bukkitEnabledCheckBox,
            spigotEnabledCheckBox,
            paperEnabledCheckBox,
            spongeEnabledCheckBox,
            forgeEnabledCheckBox,
            liteloaderEnabledCheckBox,
            mcpEnabledCheckBox,
            mixinEnabledCheckBox,
            bungeecordEnabledCheckBox,
            waterfallEnabledCheckBox
        )
    }

    private val autoCheckBoxArray: Array<JCheckBox> by lazy {
        arrayOf(
            bukkitAutoCheckBox,
            spigotAutoCheckBox,
            paperAutoCheckBox,
            spongeAutoCheckBox,
            forgeAutoCheckBox,
            liteloaderAutoCheckBox,
            mcpAutoCheckBox,
            mixinAutoCheckBox,
            bungeecordAutoCheckBox,
            waterfallAutoCheckBox
        )
    }

    override fun createComponent(): JComponent {
        if (UIUtil.isUnderDarcula()) {
            spongeIcon.icon = PlatformAssets.SPONGE_ICON_2X_DARK
            mcpIcon.icon = PlatformAssets.MCP_ICON_2X_DARK
            mixinIcon.icon = PlatformAssets.MIXIN_ICON_2X_DARK
        }

        runOnAll { enabled, auto, platformType, _, _ ->
            auto.addActionListener { checkAuto(auto, enabled, platformType) }
        }

        bukkitEnabledCheckBox.addActionListener { unique(bukkitEnabledCheckBox, spigotEnabledCheckBox, paperEnabledCheckBox) }
        spigotEnabledCheckBox.addActionListener { unique(spigotEnabledCheckBox, bukkitEnabledCheckBox, paperEnabledCheckBox) }
        paperEnabledCheckBox.addActionListener { unique(paperEnabledCheckBox, bukkitEnabledCheckBox, spigotEnabledCheckBox) }

        bukkitAutoCheckBox.addActionListener { all(bukkitAutoCheckBox, spigotAutoCheckBox, paperAutoCheckBox)(SPIGOT, PAPER) }
        spigotAutoCheckBox.addActionListener { all(spigotAutoCheckBox, bukkitAutoCheckBox, paperAutoCheckBox)(BUKKIT, PAPER) }
        paperAutoCheckBox.addActionListener { all(paperAutoCheckBox, bukkitAutoCheckBox, spigotAutoCheckBox)(BUKKIT, SPIGOT) }

        forgeEnabledCheckBox.addActionListener { also(forgeEnabledCheckBox, mcpEnabledCheckBox) }
        liteloaderEnabledCheckBox.addActionListener { also(liteloaderEnabledCheckBox, mcpEnabledCheckBox) }
        mixinEnabledCheckBox.addActionListener { also(mixinEnabledCheckBox, mcpEnabledCheckBox) }

        bungeecordEnabledCheckBox.addActionListener { unique(bungeecordEnabledCheckBox, waterfallEnabledCheckBox) }
        waterfallEnabledCheckBox.addActionListener { unique(waterfallEnabledCheckBox, bungeecordEnabledCheckBox) }

        return panel
    }

    override fun getDisplayName() = "Minecraft Module Settings"

    override fun isModified(): Boolean {
        var modified = false

        runOnAll { enabled, auto, platformType, userTypes, _ ->
            modified += auto.isSelected == platformType in userTypes
            modified += !auto.isSelected && enabled.isSelected != userTypes[platformType]
        }

        return modified
    }

    override fun reset() {
        runOnAll { enabled, auto, platformType, userTypes, autoTypes ->
            auto.isSelected = platformType !in userTypes
            enabled.isSelected = userTypes[platformType] ?: (platformType in autoTypes)

            if (auto.isSelected) {
                enabled.isEnabled = false
            }
        }
    }

    override fun apply() {
        configuration.state.userChosenTypes.clear()
        runOnAll { enabled, auto, platformType, userTypes, _ ->
            if (!auto.isSelected) {
                userTypes[platformType] = enabled.isSelected
            }
        }
    }

    private inline fun runOnAll(run: (JCheckBox, JCheckBox, PlatformType, MutableMap<PlatformType, Boolean>, Set<PlatformType>) -> Unit) {
        val state = configuration.state
        for (i in indexes) {
            run(enableCheckBoxArray[i], autoCheckBoxArray[i], platformTypes[i], state.userChosenTypes, state.autoDetectTypes)
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

    private fun all(vararg checkBoxes: JCheckBox): Invoker {
        if (checkBoxes.size <= 1) {
            return Invoker()
        }

        for (i in 1 until checkBoxes.size) {
            checkBoxes[i].isSelected = checkBoxes[0].isSelected
        }

        return object : Invoker() {
            override fun invoke(vararg indexes: Int) {
                for (i in indexes) {
                    checkAuto(autoCheckBoxArray[i], enableCheckBoxArray[i], platformTypes[i])
                }
            }
        }
    }

    private fun checkAuto(auto: JCheckBox, enabled: JCheckBox, type: PlatformType) {
        if (auto.isSelected){
            enabled.isEnabled = false
            enabled.isSelected = type in configuration.state.autoDetectTypes
        } else {
            enabled.isEnabled = true
        }
    }

    private operator fun Boolean.plus(n: Boolean) = this || n
    // This is here so we can use vararg. Can't use parameter modifiers in function type definitions for some reason
    open class Invoker { open operator fun invoke(vararg indexes: Int) {} }

    companion object {
        private const val BUKKIT = 0
        private const val SPIGOT = BUKKIT + 1
        private const val PAPER = SPIGOT + 1
        private const val SPONGE = PAPER + 1
        private const val FORGE = SPONGE + 1
        private const val LITELOADER  = FORGE + 1
        private const val MCP = LITELOADER + 1
        private const val MIXIN = MCP + 1
        private const val BUNGEECORD = MIXIN + 1
        private const val WATERFALL = BUNGEECORD + 1

        private val platformTypes = arrayOf(
            PlatformType.BUKKIT,
            PlatformType.SPIGOT,
            PlatformType.PAPER,
            PlatformType.SPONGE,
            PlatformType.FORGE,
            PlatformType.LITELOADER,
            PlatformType.MCP,
            PlatformType.MIXIN,
            PlatformType.BUNGEECORD,
            PlatformType.WATERFALL
        )

        private val indexes = intArrayOf(BUKKIT, SPIGOT, PAPER, SPONGE, FORGE, LITELOADER, MCP, MIXIN, BUNGEECORD, WATERFALL)
    }
}
