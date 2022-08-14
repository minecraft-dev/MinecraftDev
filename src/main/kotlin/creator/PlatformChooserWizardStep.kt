/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
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
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.UIUtil
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class PlatformChooserWizardStep(private val creator: MinecraftProjectCreator) : ModuleWizardStep() {

    private lateinit var panel: JPanel

    private lateinit var projectButtons: ButtonGroup
    private lateinit var spongeIcon: JLabel
    private lateinit var bukkitPluginButton: JBRadioButton
    private lateinit var spigotPluginButton: JBRadioButton
    private lateinit var paperPluginButton: JBRadioButton
    private lateinit var spongePluginButton: JBRadioButton
    private lateinit var forgeModButton: JBRadioButton
    private lateinit var fabricModButton: JBRadioButton
    private lateinit var architecturyModButton: JBRadioButton
    private lateinit var bungeeCordPluginButton: JBRadioButton
    private lateinit var waterfallPluginButton: JBRadioButton
    private lateinit var velocityPluginButton: JBRadioButton
    private lateinit var liteLoaderModButton: JBRadioButton

    override fun getComponent(): JComponent {
        if (UIUtil.isUnderDarcula()) {
            spongeIcon.icon = PlatformAssets.SPONGE_ICON_2X_DARK
        } else {
            spongeIcon.icon = PlatformAssets.SPONGE_ICON_2X
        }

        return panel
    }

    override fun updateDataModel() {
        creator.config = buildConfig()
    }

    override fun validate(): Boolean {
        updateDataModel()
        val isValid = projectButtons.selection != null
        if (isValid && creator.config == null) {
            throw IllegalStateException(
                "A project button does not have an associated config! Make sure to add your button to buildConfig()"
            )
        }
        return isValid
    }

    private fun buildConfig(): ProjectConfig? {
        return when {
            bukkitPluginButton.isSelected -> BukkitProjectConfig(PlatformType.BUKKIT)
            spigotPluginButton.isSelected -> BukkitProjectConfig(PlatformType.SPIGOT)
            paperPluginButton.isSelected -> BukkitProjectConfig(PlatformType.PAPER)
            spongePluginButton.isSelected -> SpongeProjectConfig()
            forgeModButton.isSelected -> ForgeProjectConfig()
            fabricModButton.isSelected -> FabricProjectConfig()
            architecturyModButton.isSelected -> ArchitecturyProjectConfig()
            liteLoaderModButton.isSelected -> LiteLoaderProjectConfig()
            bungeeCordPluginButton.isSelected -> BungeeCordProjectConfig(PlatformType.BUNGEECORD)
            waterfallPluginButton.isSelected -> BungeeCordProjectConfig(PlatformType.WATERFALL)
            velocityPluginButton.isSelected -> VelocityProjectConfig()
            else -> null
        }
    }
}
