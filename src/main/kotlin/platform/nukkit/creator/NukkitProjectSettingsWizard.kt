/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.nukkit.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.MinecraftModuleWizardStep
import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.creator.ValidatedField
import com.demonwav.mcdev.creator.ValidatedFieldType.CLASS_NAME
import com.demonwav.mcdev.creator.ValidatedFieldType.LIST
import com.demonwav.mcdev.creator.ValidatedFieldType.NON_BLANK
import com.demonwav.mcdev.platform.nukkit.data.LoadOrder
import com.demonwav.mcdev.util.firstOfType
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class NukkitProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

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
    private lateinit var apiVersionBox: JComboBox<String>

    private var config: NukkitProjectConfig? = null

    override fun getComponent(): JComponent {
        return panel
    }

    override fun isStepVisible(): Boolean {
        return creator.configs.any { it is NukkitProjectConfig }
    }

    override fun updateStep() {
        config = creator.configs.firstOfType()
        if (config == null) {
            return
        }
        val conf = config ?: return

        basicUpdateStep(creator, conf, pluginNameField, mainClassField)

        title.icon = PlatformAssets.NUKKIT_ICON_2Z
        title.text = "<html><font size=\"5\">Nukkit Settings</font></html>"

        apiVersionBox.addItem("1.0")
        apiVersionBox.addItem("2.0.0")
    }

    override fun validate(): Boolean {
        return super.validate() && apiVersionBox.selectedItem != null
    }

    override fun updateDataModel() {
        val conf = this.config ?: return

        conf.pluginName = this.pluginNameField.text
        conf.mainClass = this.mainClassField.text
        conf.description = this.descriptionField.text
        conf.website = this.websiteField.text

        conf.loadOrder = if (this.loadOrderBox.selectedIndex == 0) LoadOrder.POSTWORLD else LoadOrder.STARTUP
        conf.prefix = this.prefixField.text
        conf.minecraftVersion = this.apiVersionBox.selectedItem as String

        conf.setLoadBefore(this.loadBeforeField.text)
        conf.setAuthors(this.authorsField.text)
        conf.setDependencies(this.dependField.text)
        conf.setSoftDependencies(this.softDependField.text)
    }
}
