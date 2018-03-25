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
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
import com.demonwav.mcdev.platform.sponge.getSpongeVersionSelector
import com.intellij.util.ui.UIUtil
import org.apache.commons.lang.WordUtils
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class SpongeProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var pluginNameField: JTextField
    private lateinit var pluginVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var panel: JPanel
    private lateinit var title: JLabel
    private lateinit var descriptionField: JTextField
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField
    private lateinit var dependField: JTextField
    private lateinit var spongeApiVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var settings: SpongeProjectConfiguration? = null

    override fun getComponent(): JComponent {
        settings = creator.settings[PlatformType.SPONGE] as? SpongeProjectConfiguration
        if (settings == null) {
            return panel
        }

        val name = WordUtils.capitalize(creator.artifactId.replace('-', ' '))
        pluginNameField.text = name
        pluginVersionField.text = creator.version

        if (settings != null && !settings!!.isFirst) {
            pluginNameField.isEditable = false
            pluginVersionField.isEditable = false
        }

        mainClassField.text = creator.groupId.replace("-", "").toLowerCase() + "." +
            creator.artifactId.replace("-", "").toLowerCase() + "." + name.replace(" ", "")

        if (creator.settings.size > 1) {
            mainClassField.text = mainClassField.text + PlatformType.SPONGE.normalName
        }

        if (UIUtil.isUnderDarcula()) {
            title.icon = PlatformAssets.SPONGE_ICON_2X_DARK
        } else {
            title.icon = PlatformAssets.SPONGE_ICON_2X
        }

        getSpongeVersionSelector()
            .onSuccess { it.set(spongeApiVersionBox) }
            .onError { errorLabel.isVisible = true }

        return panel
    }

    override fun validate(): Boolean {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField, MinecraftModuleWizardStep.pattern) &&
            spongeApiVersionBox.selectedItem != null
    }

    override fun isStepVisible(): Boolean {
        settings = creator.settings[PlatformType.SPONGE] as? SpongeProjectConfiguration
        return settings != null
    }

    override fun onStepLeaving() {
        settings!!.pluginName = pluginNameField.text
        settings!!.pluginVersion = pluginVersionField.text
        settings!!.mainClass = mainClassField.text

        settings!!.setAuthors(authorsField.text)
        settings!!.setDependencies(dependField.text)
        settings!!.description = descriptionField.text
        settings!!.website = websiteField.text

        settings!!.spongeApiVersion = spongeApiVersionBox.selectedItem as? String ?: ""
    }

    override fun updateDataModel() {}
}
