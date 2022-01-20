/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.architectury.creator.ArchitecturyProjectConfig
import com.demonwav.mcdev.platform.bukkit.creator.BukkitProjectConfig
import com.demonwav.mcdev.platform.bungeecord.creator.BungeeCordProjectConfig
import com.demonwav.mcdev.platform.fabric.creator.FabricProjectConfig
import com.demonwav.mcdev.platform.forge.creator.ForgeProjectConfig
import com.demonwav.mcdev.platform.liteloader.creator.LiteLoaderProjectConfig
import com.demonwav.mcdev.platform.sponge.creator.SpongeProjectConfig
import com.demonwav.mcdev.platform.velocity.creator.VelocityProjectConfig
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.LightColors
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class PlatformChooserWizardStep(private val creator: MinecraftProjectCreator) : ModuleWizardStep() {

    private lateinit var chooserPanel: JPanel
    private lateinit var panel: JPanel

    private lateinit var spongeIcon: JLabel
    private lateinit var bukkitPluginCheckBox: JCheckBox
    private lateinit var spigotPluginCheckBox: JCheckBox
    private lateinit var paperPluginCheckBox: JCheckBox
    private lateinit var spongePluginCheckBox: JCheckBox
    private lateinit var forgeModCheckBox: JCheckBox
    private lateinit var fabricModCheckBox: JCheckBox
    private lateinit var architecturyModCheckBox: JCheckBox
    private lateinit var bungeeCordPluginCheckBox: JCheckBox
    private lateinit var waterfallPluginCheckBox: JCheckBox
    private lateinit var velocityPluginCheckBox: JCheckBox
    private lateinit var liteLoaderModCheckBox: JCheckBox

    override fun getComponent(): JComponent {
        // Set types
        bukkitPluginCheckBox.addActionListener {
            toggle(
                bukkitPluginCheckBox,
                spigotPluginCheckBox,
                paperPluginCheckBox
            )
        }
        spigotPluginCheckBox.addActionListener {
            toggle(
                spigotPluginCheckBox,
                bukkitPluginCheckBox,
                paperPluginCheckBox
            )
        }
        paperPluginCheckBox.addActionListener {
            toggle(
                paperPluginCheckBox,
                bukkitPluginCheckBox,
                spigotPluginCheckBox
            )
        }
        forgeModCheckBox.addActionListener { toggle(forgeModCheckBox, liteLoaderModCheckBox, architecturyModCheckBox) }
        fabricModCheckBox.addActionListener { toggle(fabricModCheckBox, architecturyModCheckBox) }
        architecturyModCheckBox.addActionListener {
            toggle(
                architecturyModCheckBox,
                fabricModCheckBox,
                forgeModCheckBox
            )
        }
        liteLoaderModCheckBox.addActionListener { toggle(liteLoaderModCheckBox, forgeModCheckBox) }
        bungeeCordPluginCheckBox.addActionListener { toggle(bungeeCordPluginCheckBox, waterfallPluginCheckBox) }
        waterfallPluginCheckBox.addActionListener { toggle(waterfallPluginCheckBox, bungeeCordPluginCheckBox) }

        if (UIUtil.isUnderDarcula()) {
            spongeIcon.icon = PlatformAssets.SPONGE_ICON_2X_DARK
        } else {
            spongeIcon.icon = PlatformAssets.SPONGE_ICON_2X
        }

        return panel
    }

    private fun toggle(one: JCheckBox, vararg others: JCheckBox) {
        if (one.isSelected) {
            others.forEach { it.isSelected = false }
        }
    }

    override fun updateDataModel() {
        creator.configs.clear()
        creator.configs.addAll(buildConfigs())
    }

    override fun validate(): Boolean {
        val currentConfigs = buildConfigs()
        val validBuildSystemTypes = BuildSystemType.values()
            .count { type -> currentConfigs.all { type.creatorType.isInstance(it) } }

        if (validBuildSystemTypes == 0) {
            val message = "This project configuration is not valid, please choose a different set of platforms"
            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, null, LightColors.RED, null)
                .setHideOnAction(true)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .createBalloon()

            balloon.show(RelativePoint.getSouthOf(chooserPanel), Balloon.Position.atRight)
            return false
        }

        return bukkitPluginCheckBox.isSelected ||
            spigotPluginCheckBox.isSelected ||
            paperPluginCheckBox.isSelected ||
            spongePluginCheckBox.isSelected ||
            forgeModCheckBox.isSelected ||
            fabricModCheckBox.isSelected ||
            architecturyModCheckBox.isSelected ||
            liteLoaderModCheckBox.isSelected ||
            bungeeCordPluginCheckBox.isSelected ||
            waterfallPluginCheckBox.isSelected ||
            velocityPluginCheckBox.isSelected
    }

    private fun buildConfigs(): LinkedHashSet<ProjectConfig> {
        val result = LinkedHashSet<ProjectConfig>()
        if (bukkitPluginCheckBox.isSelected) {
            result += BukkitProjectConfig(PlatformType.BUKKIT)
        }

        if (spigotPluginCheckBox.isSelected) {
            result += BukkitProjectConfig(PlatformType.SPIGOT)
        }

        if (paperPluginCheckBox.isSelected) {
            result += BukkitProjectConfig(PlatformType.PAPER)
        }

        if (spongePluginCheckBox.isSelected) {
            result += SpongeProjectConfig()
        }

        if (forgeModCheckBox.isSelected) {
            result += ForgeProjectConfig()
        }

        if (fabricModCheckBox.isSelected) {
            result += FabricProjectConfig()
        }

        if (architecturyModCheckBox.isSelected) {
            result += ArchitecturyProjectConfig()
        }

        if (liteLoaderModCheckBox.isSelected) {
            result += LiteLoaderProjectConfig()
        }

        if (bungeeCordPluginCheckBox.isSelected) {
            result += BungeeCordProjectConfig(PlatformType.BUNGEECORD)
        }

        if (waterfallPluginCheckBox.isSelected) {
            result += BungeeCordProjectConfig(PlatformType.WATERFALL)
        }

        if (velocityPluginCheckBox.isSelected) {
            result += VelocityProjectConfig()
        }

        return result
    }
}
