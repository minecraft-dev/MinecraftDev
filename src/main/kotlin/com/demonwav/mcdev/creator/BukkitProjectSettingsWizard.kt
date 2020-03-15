/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

@file:Suppress("Duplicates")

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder
import com.demonwav.mcdev.util.firstOfType
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.apache.commons.lang.WordUtils

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

    private var config: BukkitProjectConfiguration? = null

    override fun getComponent(): JComponent {
        return panel
    }

    override fun isStepVisible(): Boolean {
        return creator.configs.any { it is BukkitProjectConfiguration }
    }

    override fun updateStep() {
        config = creator.configs.firstOfType()
        if (config == null) {
            return
        }

        val buildSystem = creator.buildSystem ?: return

        val name = WordUtils.capitalize(buildSystem.artifactId.replace('-', ' '))
        pluginNameField.text = name
        pluginVersionField.text = buildSystem.version

        val conf = config ?: return

        if (creator.configs.indexOf(conf) != 0) {
            pluginNameField.isEditable = false
            pluginVersionField.isEditable = false
        }

        mainClassField.text = buildSystem.groupId.replace("-", "").toLowerCase() + "." +
            buildSystem.artifactId.replace("-", "").toLowerCase() + "." + name.replace(" ", "")

        if (creator.configs.size > 1) {
            mainClassField.text = mainClassField.text + conf.type.normalName
        }

        when (conf.type) {
            PlatformType.BUKKIT -> {
                title.icon = PlatformAssets.BUKKIT_ICON_2X
                title.text = "<html><font size=\"5\">Bukkit Settings</font></html>"
            }
            PlatformType.SPIGOT -> {
                title.icon = PlatformAssets.SPIGOT_ICON_2X
                title.text = "<html><font size=\"5\">Spigot Settings</font></html>"
            }
            PlatformType.PAPER -> {
                title.icon = PlatformAssets.PAPER_ICON_2X
                title.text = "<html><font size=\"5\">Paper Settings</font></html>"
            }
            else -> {
            }
        }

        CoroutineScope(Dispatchers.Swing).launch {
            try {
                withContext(Dispatchers.IO) { getVersionSelector(conf.type) }.set(minecraftVersionBox)
            } catch (e: Exception) {
                errorLabel.isVisible = true
            }
        }
    }

    override fun validate(): Boolean {
        return validate(
            pluginNameField,
            pluginVersionField,
            mainClassField,
            authorsField,
            dependField,
            pattern
        ) && minecraftVersionBox.selectedItem != null
    }

    override fun onStepLeaving() {
        val conf = config ?: return
        conf.data = BukkitProjectConfiguration.BukkitData(
            loadOrder = if (loadOrderBox.selectedIndex == 0) LoadOrder.POSTWORLD else LoadOrder.STARTUP,
            prefix = prefixField.text,
            minecraftVersion = minecraftVersionBox.selectedItem as? String ?: ""
        )
        conf.setLoadBefore(loadBeforeField.text)

        conf.base = ProjectConfiguration.BaseConfigs(
            pluginNameField.text,
            pluginVersionField.text,
            mainClassField.text,
            descriptionField.text,
            websiteField.text
        )

        conf.setAuthors(this.authorsField.text)
        conf.setDependencies(this.dependField.text)
        conf.setSoftDependencies(this.softDependField.text)
    }

    override fun updateDataModel() {}
}
