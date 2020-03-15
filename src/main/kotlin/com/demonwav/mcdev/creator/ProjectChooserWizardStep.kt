/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.liteloader.LiteLoaderProjectConfiguration
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.UIUtil
import java.awt.Desktop
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

class ProjectChooserWizardStep(private val creator: MinecraftProjectCreator) : ModuleWizardStep() {

    private lateinit var chooserPanel: JPanel
    private lateinit var panel: JPanel
    private lateinit var infoPanel: JPanel

    private lateinit var infoPane: JEditorPane
    private lateinit var spongeIcon: JLabel
    private lateinit var bukkitPluginCheckBox: JCheckBox
    private lateinit var spigotPluginCheckBox: JCheckBox
    private lateinit var paperPluginCheckBox: JCheckBox
    private lateinit var spongePluginCheckBox: JCheckBox
    private lateinit var forgeModCheckBox: JCheckBox
    private lateinit var bungeeCordPluginCheckBox: JCheckBox
    private lateinit var waterfallPluginCheckBox: JCheckBox
    private lateinit var liteLoaderModCheckBox: JCheckBox

    override fun getComponent(): JComponent {
        chooserPanel.border = IdeBorderFactory.createBorder()
        infoPanel.border = IdeBorderFactory.createBorder()

        // HTML parsing and hyperlink support
        infoPane.contentType = "text/html"
        infoPane.addHyperlinkListener { e ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(e.url.toURI())
                }
            }
        }

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
        spongePluginCheckBox.addActionListener { fillInInfoPane() }
        forgeModCheckBox.addActionListener { fillInInfoPane() }
        liteLoaderModCheckBox.addActionListener { fillInInfoPane() }
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
        fillInInfoPane()
    }

    private fun fillInInfoPane() {
        val sb = StringBuilder("<html><font size=\"4\">")

        fun StringBuilder.append(checkbox: JCheckBox, text: String) {
            if (checkbox.isSelected) {
                append(text)
                append("<p/>")
            }
        }

        sb.append(bukkitPluginCheckBox, bukkitInfo)
        sb.append(spigotPluginCheckBox, spigotInfo)
        sb.append(paperPluginCheckBox, paperInfo)
        sb.append(spongePluginCheckBox, spongeInfo)
        sb.append(forgeModCheckBox, forgeInfo)
        sb.append(liteLoaderModCheckBox, liteLoaderInfo)
        sb.append(bungeeCordPluginCheckBox, bungeeCordInfo)
        sb.append(waterfallPluginCheckBox, waterfallInfo)

        sb.append("</font></html>")

        infoPane.text = sb.toString()
    }

    override fun updateDataModel() {
        creator.configs.clear()

        if (bukkitPluginCheckBox.isSelected) {
            creator.configs += BukkitProjectConfiguration(PlatformType.BUKKIT)
        }

        if (spigotPluginCheckBox.isSelected) {
            creator.configs += BukkitProjectConfiguration(PlatformType.SPIGOT)
        }

        if (paperPluginCheckBox.isSelected) {
            creator.configs += BukkitProjectConfiguration(PlatformType.PAPER)
        }

        if (spongePluginCheckBox.isSelected) {
            creator.configs += SpongeProjectConfiguration()
        }

        if (forgeModCheckBox.isSelected) {
            creator.configs += ForgeProjectConfiguration()
        }

        if (liteLoaderModCheckBox.isSelected) {
            creator.configs += LiteLoaderProjectConfiguration()
        }

        if (bungeeCordPluginCheckBox.isSelected) {
            creator.configs += BungeeCordProjectConfiguration(PlatformType.BUNGEECORD)
        }

        if (waterfallPluginCheckBox.isSelected) {
            creator.configs += BungeeCordProjectConfiguration(PlatformType.WATERFALL)
        }
    }

    override fun validate(): Boolean {
        return bukkitPluginCheckBox.isSelected ||
            spigotPluginCheckBox.isSelected ||
            paperPluginCheckBox.isSelected ||
            spongePluginCheckBox.isSelected ||
            forgeModCheckBox.isSelected ||
            liteLoaderModCheckBox.isSelected ||
            bungeeCordPluginCheckBox.isSelected ||
            waterfallPluginCheckBox.isSelected
    }

    companion object {
        private const val bukkitInfo = "Create a standard " +
            "<a href=\"https://bukkit.org/\">Bukkit</a> plugin, for use " +
            "on CraftBukkit, Spigot, and Paper servers."
        private const val spigotInfo = "Create a standard " +
            "<a href=\"https://www.spigotmc.org/\">Spigot</a> plugin, for use " +
            "on Spigot and Paper servers."
        private const val paperInfo = "Create a standard " +
            "<a href=\"https://paper.emc.gs\">Paper</a> plugin, for use " +
            "on Paper servers."
        private const val bungeeCordInfo = "Create a standard " +
            "<a href=\"https://www.spigotmc.org/wiki/bungeecord/\">BungeeCord</a> plugin, for use " +
            "on BungeeCord and Waterfall servers."
        private const val waterfallInfo = "Create a standard " +
            "<a href=\"https://aquifermc.org/\">Waterfall</a> plugin, for use " +
            "on Waterfall servers."
        private const val spongeInfo = "Create a standard " +
            "<a href=\"https://www.spongepowered.org/\">Sponge</a> plugin, for use " +
            "on Sponge servers."
        private const val forgeInfo = "Create a standard " +
            "<a href=\"https://files.minecraftforge.net/\">Forge</a> mod, for use " +
            "on Forge servers and clients."
        private const val liteLoaderInfo = "Create a standard " +
            "<a href=\"http://www.liteloader.com/\">LiteLoader</a> mod, for use " +
            "on LiteLoader clients."
    }
}
