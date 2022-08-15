/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.MinecraftModuleWizardStep
import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.creator.ValidatedField
import com.demonwav.mcdev.creator.ValidatedFieldType.CLASS_NAME
import com.demonwav.mcdev.creator.ValidatedFieldType.LIST
import com.demonwav.mcdev.creator.ValidatedFieldType.NON_BLANK
import com.demonwav.mcdev.creator.getVersionSelector
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder
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

class BukkitProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    @ValidatedField(NON_BLANK)
    private lateinit var pluginNameField: JTextField

    @ValidatedField(NON_BLANK, CLASS_NAME)
    private lateinit var mainClassField: JTextField
    private lateinit var panel: JPanel
    private lateinit var descriptionField: JTextField

    @ValidatedField(LIST)
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField
    private lateinit var prefixField: JTextField
    private lateinit var loadOrderBox: JComboBox<*>
    private lateinit var loadBeforeField: JTextField

    @ValidatedField(LIST)
    private lateinit var dependField: JTextField
    private lateinit var softDependField: JTextField
    private lateinit var title: JLabel
    private lateinit var minecraftVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var config: BukkitProjectConfig? = null

    private var versionsLoaded: Boolean = false

    override fun getComponent(): JComponent {
        return panel
    }

    override fun isStepVisible(): Boolean {
        return creator.config is BukkitProjectConfig
    }

    override fun updateStep() {
        config = creator.config as? BukkitProjectConfig
        if (config == null) {
            return
        }
        val conf = config ?: return

        basicUpdateStep(creator, pluginNameField, mainClassField)

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

        if (versionsLoaded) {
            return
        }

        versionsLoaded = true
        CoroutineScope(Dispatchers.Swing).launch {
            try {
                withContext(Dispatchers.IO) { getVersionSelector(conf.type) }.set(minecraftVersionBox)
            } catch (e: Exception) {
                errorLabel.isVisible = true
            }
        }
    }

    override fun validate(): Boolean {
        return super.validate() && minecraftVersionBox.selectedItem != null
    }

    override fun updateDataModel() {
        val conf = this.config ?: return

        conf.pluginName = this.pluginNameField.text
        conf.mainClass = this.mainClassField.text
        conf.description = this.descriptionField.text
        conf.website = this.websiteField.text

        conf.loadOrder = if (this.loadOrderBox.selectedIndex == 0) LoadOrder.POSTWORLD else LoadOrder.STARTUP
        conf.prefix = this.prefixField.text
        conf.minecraftVersion = this.minecraftVersionBox.selectedItem as String

        conf.setLoadBefore(this.loadBeforeField.text)
        conf.setAuthors(this.authorsField.text)
        conf.setDependencies(this.dependField.text)
        conf.setSoftDependencies(this.softDependField.text)
    }
}
