/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.MCMessages
import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder
import org.apache.commons.lang.WordUtils
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class BukkitProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var pluginNameField: JTextField
    private lateinit var pluginVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var panel: JPanel
    private lateinit var descriptionField: JTextField
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField
    private lateinit var prefixField: JTextField
    private lateinit var loadOrderBox: JComboBox<*>
    private lateinit var loadBeforeField: JTextField
    private lateinit var dependField: JTextField
    private lateinit var softDependField: JTextField
    private lateinit var title: JLabel
    private lateinit var minecraftVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var settings: BukkitProjectConfiguration? = null

    override fun getComponent(): JComponent {
        settings = creator.settings[PlatformType.BUKKIT] as? BukkitProjectConfiguration
        if (settings == null) {
            return panel
        }

        val name = WordUtils.capitalize(creator.artifactId)
        pluginNameField.text = name
        pluginVersionField.text = creator.version

        if (settings != null && !settings!!.isFirst) {
            pluginNameField.isEditable = false
            pluginVersionField.isEditable = false
        }

        mainClassField.text = "${creator.groupId.toLowerCase()}.${creator.artifactId.toLowerCase()}.$name"

        if (creator.settings.size > 1) {
            mainClassField.text = mainClassField.text + settings!!.type.normalName
        }

        when (settings!!.type) {
            PlatformType.BUKKIT -> {
                title.icon = PlatformAssets.BUKKIT_ICON_2X
                title.text = MCMessages["setup.settings.bukkit"]
            }
            PlatformType.SPIGOT -> {
                title.icon = PlatformAssets.SPIGOT_ICON_2X
                title.text = MCMessages["setup.settings.spigot"]
            }
            PlatformType.PAPER -> {
                title.icon = PlatformAssets.PAPER_ICON_2X
                title.text = MCMessages["setup.settings.paper"]
            }
            else -> {}
        }

        getVersionSelector(settings!!.type)
            .done { it.set(minecraftVersionBox) }
            .rejected { errorLabel.isVisible = true }

        return panel
    }

    override fun isStepVisible(): Boolean {
        settings = creator.settings[PlatformType.BUKKIT] as? BukkitProjectConfiguration
        return settings != null
    }

    override fun validate(): Boolean {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField, MinecraftModuleWizardStep.pattern) &&
            minecraftVersionBox.selectedItem != null
    }

    override fun onStepLeaving() {
        this.settings!!.apply {
            pluginName = pluginNameField.text
            pluginVersion = pluginVersionField.text
            mainClass = mainClassField.text
            description = descriptionField.text
            setAuthors(authorsField.text)
            website = websiteField.text
            prefix = prefixField.text
            loadOrder = if (loadOrderBox.selectedIndex == 0) LoadOrder.POSTWORLD else LoadOrder.STARTUP
            setLoadBefore(loadBeforeField.text)
            setDependencies(dependField.text)
            setSoftDependencies(softDependField.text)
            minecraftVersion = minecraftVersionBox.selectedItem as? String ?: ""
        }
    }

    override fun updateDataModel() {}
}
