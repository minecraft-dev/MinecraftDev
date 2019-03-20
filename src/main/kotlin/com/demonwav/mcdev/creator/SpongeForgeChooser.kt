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
        return panel
    }

    override fun updateDataModel() {}

    override fun updateStep() {
        if (UIUtil.isUnderDarcula()) {
            title.icon = PlatformAssets.SPONGE_FORGE_ICON_2X_DARK
        } else {
            title.icon = PlatformAssets.SPONGE_FORGE_ICON_2X
        }
    }

    override fun isStepVisible(): Boolean {
        // Only show this if both Sponge and Forge are selected
        return creator.configs.count { conf -> conf is ForgeProjectConfiguration || conf is SpongeProjectConfiguration } >= 2 ||
                creator.configs.any { conf -> conf is SpongeForgeProjectConfiguration }
    }

    override fun onStepLeaving() {
        if (singleRadioButton.isSelected) {
            creator.configs.removeIf { it is SpongeProjectConfiguration }
            if (creator.configs.none { it is SpongeForgeProjectConfiguration }) {
                creator.configs.removeIf { it is ForgeProjectConfiguration }
                // Add single SpongeForge config
                creator.configs += SpongeForgeProjectConfiguration()
            }
        } else {
            creator.configs.removeIf { it is SpongeForgeProjectConfiguration }
            // Add separate Sponge and Forge configs
            if (creator.configs.none { it is SpongeProjectConfiguration }) {
                creator.configs += SpongeProjectConfiguration()
            }
            if (creator.configs.none { it is ForgeProjectConfiguration }) {
                creator.configs += ForgeProjectConfiguration()
            }
        }
    }
}
