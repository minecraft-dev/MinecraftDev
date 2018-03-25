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
import com.demonwav.mcdev.platform.canary.CanaryProjectConfiguration
import org.apache.commons.lang.WordUtils
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CanaryProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var pluginNameField: JTextField
    private lateinit var pluginVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var panel: JPanel
    private lateinit var authorsField: JTextField
    private lateinit var loadOrderBox: JComboBox<*>
    private lateinit var dependField: JTextField
    private lateinit var title: JLabel
    private lateinit var canaryVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var settings: CanaryProjectConfiguration? = null

    override fun getComponent(): JComponent {
        settings = creator.settings[PlatformType.CANARY] as? CanaryProjectConfiguration
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
            mainClassField.text = mainClassField.text + settings!!.type.normalName
        }

        when (settings!!.type) {
            PlatformType.CANARY -> {
                title.icon = PlatformAssets.CANARY_ICON_2X
                title.text = "<html><font size=\"5\">Canary Settings</font></html>"
            }
            PlatformType.NEPTUNE -> {
                title.icon = PlatformAssets.NEPTUNE_ICON_2X
                title.text = "<html><font size=\"5\">Neptune Settings</font></html>"
            }
            else -> {}
        }

        getVersionSelector(settings!!.type)
            .onSuccess { it.set(canaryVersionBox) }
            .onError { errorLabel.isVisible = true }

        return panel
    }

    override fun isStepVisible(): Boolean {
        settings = creator.settings[PlatformType.CANARY] as? CanaryProjectConfiguration
        return settings != null
    }

    override fun validate(): Boolean {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField, MinecraftModuleWizardStep.pattern) &&
            canaryVersionBox.selectedItem != null
    }

    override fun onStepLeaving() {
        this.settings!!.pluginName = pluginNameField.text
        this.settings!!.pluginVersion = pluginVersionField.text
        this.settings!!.mainClass = mainClassField.text
        this.settings!!.setAuthors(this.authorsField.text)
        this.settings!!.isEnableEarly = this.loadOrderBox.selectedIndex != 0
        this.settings!!.setDependencies(this.dependField.text)
        this.settings!!.canaryVersion = canaryVersionBox.selectedItem as? String ?: ""
    }

    override fun updateDataModel() {}
}
