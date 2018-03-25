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
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration
import org.apache.commons.lang.WordUtils
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class BungeeCordProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var panel: JPanel
    private lateinit var pluginNameField: JTextField
    private lateinit var pluginVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var descriptionField: JTextField
    private lateinit var authorField: JTextField
    private lateinit var dependField: JTextField
    private lateinit var softDependField: JTextField
    private lateinit var title: JLabel
    private lateinit var minecraftVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var settings: BungeeCordProjectConfiguration? = null

    override fun getComponent(): JComponent {
        settings = creator.settings[PlatformType.BUNGEECORD] as? BungeeCordProjectConfiguration
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
            PlatformType.BUNGEECORD -> {
                title.icon = PlatformAssets.BUNGEECORD_ICON_2X
                title.text = "<html><font size=\"5\">BungeeCord Settings</font></html>"
            }
            PlatformType.WATERFALL -> {
                title.icon = PlatformAssets.WATERFALL_ICON_2X
                title.text = "<html><font size=\"5\">Waterfall Settings</font></html>"
            }
            else -> {}
        }

        getVersionSelector(settings!!.type)
            .onSuccess { it.set(minecraftVersionBox) }
            .onError { errorLabel.isVisible = true }

        return panel
    }

    override fun validate(): Boolean {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorField, dependField, pattern) &&
            minecraftVersionBox.selectedItem != null
    }

    override fun isStepVisible(): Boolean {
        settings = creator.settings[PlatformType.BUNGEECORD] as? BungeeCordProjectConfiguration
        return settings != null
    }

    override fun onStepLeaving() {
        this.settings!!.pluginName = pluginNameField.text
        this.settings!!.pluginVersion = pluginVersionField.text
        this.settings!!.mainClass = mainClassField.text
        this.settings!!.description = descriptionField.text
        this.settings!!.setAuthors(this.authorField.text)
        this.settings!!.setDependencies(this.dependField.text)
        this.settings!!.setSoftDependencies(this.softDependField.text)
        this.settings!!.minecraftVersion = minecraftVersionBox.selectedItem as? String ?: ""
    }

    override fun updateDataModel() {}

    companion object {
        private val pattern = "(\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*,?|\\[?\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*])?".toRegex()
    }
}
