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
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

class SpongeForgeChooser(private val creator: MinecraftProjectCreator) : ModuleWizardStep() {

    private lateinit var panel: JPanel
    private lateinit var singleRadioButton: JRadioButton
    private lateinit var title: JLabel

    override fun getComponent(): JComponent {
        if (UIUtil.isUnderDarcula()) {
            title.icon = PlatformAssets.SPONGE_FORGE_ICON_2X_DARK
        } else {
            title.icon = PlatformAssets.SPONGE_FORGE_ICON_2X
        }

        return panel
    }

    override fun updateDataModel() {}

    override fun isStepVisible(): Boolean {
        // Only show this if both Sponge and Forge are selected
        val values = creator.settings.values
        return values.count { conf -> conf is ForgeProjectConfiguration || conf is SpongeProjectConfiguration } >= 2 ||
            values.any { conf -> conf is SpongeForgeProjectConfiguration }
    }

    override fun onStepLeaving() {
        if (singleRadioButton.isSelected) {
            // First remove the singular forge and sponge configurations
            creator.settings
                .values
                .removeIf { configuration -> configuration is ForgeProjectConfiguration || configuration is SpongeProjectConfiguration }

            // Now add the combined SpongeForgeProjectConfiguration only if it's not already there
            if (creator.settings.values.none { configuration -> configuration is SpongeForgeProjectConfiguration }) {
                creator.settings[PlatformType.FORGE] = SpongeForgeProjectConfiguration()
            }
        } else {
            // First remove the multi sponge forge configuration
            creator.settings.values.removeIf { configuration -> configuration is SpongeForgeProjectConfiguration }

            // Now add Forge and Sponge configurations respectively, but only if they aren't already there
            if (creator.settings.values.none { configuration -> configuration is ForgeProjectConfiguration }) {
                creator.settings[PlatformType.FORGE] = ForgeProjectConfiguration()
            }
            if (creator.settings.values.none { configuration -> configuration is SpongeProjectConfiguration }) {
                creator.settings[PlatformType.SPONGE] = SpongeProjectConfiguration()
            }
        }
    }
}
