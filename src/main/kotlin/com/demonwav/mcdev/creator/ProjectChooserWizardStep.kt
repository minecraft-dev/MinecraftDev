/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration
import com.demonwav.mcdev.platform.canary.CanaryProjectConfiguration
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.liteloader.LiteLoaderProjectConfiguration
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.IdeBorderFactory
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
    private lateinit var canaryPluginCheckBox: JCheckBox
    private lateinit var neptunePluginCheckBox: JCheckBox

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
        bukkitPluginCheckBox.addActionListener { toggle(bukkitPluginCheckBox, spigotPluginCheckBox, paperPluginCheckBox) }
        spigotPluginCheckBox.addActionListener { toggle(spigotPluginCheckBox, bukkitPluginCheckBox, paperPluginCheckBox) }
        paperPluginCheckBox.addActionListener { toggle(paperPluginCheckBox, bukkitPluginCheckBox, spigotPluginCheckBox) }
        spongePluginCheckBox.addActionListener { fillInInfoPane() }
        forgeModCheckBox.addActionListener { fillInInfoPane() }
        liteLoaderModCheckBox.addActionListener { fillInInfoPane() }
        bungeeCordPluginCheckBox.addActionListener { toggle(bungeeCordPluginCheckBox, waterfallPluginCheckBox) }
        waterfallPluginCheckBox.addActionListener { toggle(waterfallPluginCheckBox, bungeeCordPluginCheckBox) }
        canaryPluginCheckBox.addActionListener { toggle(canaryPluginCheckBox, neptunePluginCheckBox) }
        neptunePluginCheckBox.addActionListener { toggle(neptunePluginCheckBox, canaryPluginCheckBox) }

        spongeIcon.icon = PlatformAssets.SPONGE_ICON_2X

        return panel
    }

    private fun toggle(one: JCheckBox, vararg others: JCheckBox) {
        if (one.isSelected) {
            others.forEach { it.isSelected = false }
        }
        fillInInfoPane()
    }

    private fun fillInInfoPane() {
        var text = "<html><font size=\"4\">"

        if (bukkitPluginCheckBox.isSelected) {
            text += bukkitInfo
            text += "<p/>"
        }

        if (spigotPluginCheckBox.isSelected) {
            text += spigotInfo
            text += "<p/>"
        }

        if (paperPluginCheckBox.isSelected) {
            text += paperInfo
            text += "<p/>"
        }

        if (spongePluginCheckBox.isSelected) {
            text += spongeInfo
            text += "<p/>"
        }

        if (forgeModCheckBox.isSelected) {
            text += forgeInfo
            text += "<p/>"
        }

        if (liteLoaderModCheckBox.isSelected) {
            text += liteLoaderInfo
            text += "<p/>"
        }

        if (bungeeCordPluginCheckBox.isSelected) {
            text += bungeeCordInfo
            text += "<p/>"
        }

        if (waterfallPluginCheckBox.isSelected) {
            text += waterfallInfo
            text += "<p/>"
        }

        if (canaryPluginCheckBox.isSelected) {
            text += canaryInfo
            text += "<p/>"
        }

        if (neptunePluginCheckBox.isSelected) {
            text += neptuneInfo
        }

        text += "</font></html>"

        infoPane.text = text
    }

    override fun updateDataModel() {
        creator.settings.clear()

        if (bukkitPluginCheckBox.isSelected) {
            val configuration = BukkitProjectConfiguration()
            configuration.type = PlatformType.BUKKIT
            creator.settings[PlatformType.BUKKIT] = configuration
        }

        if (spigotPluginCheckBox.isSelected) {
            val configuration = BukkitProjectConfiguration()
            configuration.type = PlatformType.SPIGOT
            creator.settings[PlatformType.BUKKIT] = configuration
        }

        if (paperPluginCheckBox.isSelected) {
            val configuration = BukkitProjectConfiguration()
            configuration.type = PlatformType.PAPER
            creator.settings[PlatformType.BUKKIT] = configuration
        }

        if (spongePluginCheckBox.isSelected) {
            creator.settings[PlatformType.SPONGE] = SpongeProjectConfiguration()
        }

        if (forgeModCheckBox.isSelected) {
            creator.settings[PlatformType.FORGE] = ForgeProjectConfiguration()
        }

        if (liteLoaderModCheckBox.isSelected) {
            creator.settings[PlatformType.LITELOADER] = LiteLoaderProjectConfiguration()
        }

        if (bungeeCordPluginCheckBox.isSelected) {
            val configuration = BungeeCordProjectConfiguration()
            configuration.type = PlatformType.BUNGEECORD
            creator.settings[PlatformType.BUNGEECORD] = configuration
        }

        if (waterfallPluginCheckBox.isSelected) {
            val configuration = BungeeCordProjectConfiguration()
            configuration.type = PlatformType.WATERFALL
            creator.settings[PlatformType.BUNGEECORD] = configuration
        }

        if (canaryPluginCheckBox.isSelected) {
            val configuration = CanaryProjectConfiguration()
            configuration.type = PlatformType.CANARY
            creator.settings[PlatformType.CANARY] = configuration
        }

        if (neptunePluginCheckBox.isSelected) {
            val configuration = CanaryProjectConfiguration()
            configuration.type = PlatformType.NEPTUNE
            creator.settings[PlatformType.CANARY] = configuration
        }

        creator.settings.values.iterator().next().isFirst = true
    }

    override fun validate(): Boolean {
        return bukkitPluginCheckBox.isSelected ||
            spigotPluginCheckBox.isSelected ||
            paperPluginCheckBox.isSelected ||
            spongePluginCheckBox.isSelected ||
            forgeModCheckBox.isSelected ||
            liteLoaderModCheckBox.isSelected ||
            bungeeCordPluginCheckBox.isSelected ||
            waterfallPluginCheckBox.isSelected ||
            canaryPluginCheckBox.isSelected ||
            neptunePluginCheckBox.isSelected
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
        private const val canaryInfo = "Create a standard " +
            "<a href=\"https://canarymod.net/\">Canary</a> plugin, for use " +
            "on CanaryMod and Neptune servers."
        private const val neptuneInfo = "Create a standard " +
            "<a href=\"https://www.neptunepowered.org/\">Neptune</a> plugin, for use " +
            "on Neptune servers."
    }
}
