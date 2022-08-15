/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.MinecraftModuleWizardStep
import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.creator.ValidatedField
import com.demonwav.mcdev.creator.ValidatedFieldType.CLASS_NAME
import com.demonwav.mcdev.creator.ValidatedFieldType.LIST
import com.demonwav.mcdev.creator.ValidatedFieldType.NON_BLANK
import com.demonwav.mcdev.creator.getVersionSelector
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

class VelocityProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    @ValidatedField(NON_BLANK)
    private lateinit var pluginNameField: JTextField

    @ValidatedField(NON_BLANK, CLASS_NAME)
    private lateinit var mainClassField: JTextField
    private lateinit var panel: JPanel
    private lateinit var title: JLabel
    private lateinit var descriptionField: JTextField

    @ValidatedField(LIST)
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField

    @ValidatedField(LIST)
    private lateinit var dependField: JTextField
    private lateinit var velocityApiVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var config: VelocityProjectConfig? = null

    private var versionsLoaded: Boolean = false

    override fun getComponent(): JComponent {
        return panel
    }

    override fun validate(): Boolean {
        return super.validate() && velocityApiVersionBox.selectedItem != null
    }

    override fun updateStep() {
        config = creator.config as? VelocityProjectConfig
        if (config == null) {
            return
        }
        val conf = config ?: return

        basicUpdateStep(creator, pluginNameField, mainClassField)

        if (versionsLoaded) {
            return
        }

        versionsLoaded = true
        CoroutineScope(Dispatchers.Swing).launch {
            try {
                withContext(Dispatchers.IO) { getVersionSelector(conf.type) }.set(velocityApiVersionBox)
            } catch (e: Exception) {
                errorLabel.isVisible = true
            }
        }
    }

    override fun isStepVisible(): Boolean {
        return creator.config is VelocityProjectConfig
    }

    override fun updateDataModel() {
        val conf = this.config ?: return

        conf.pluginName = this.pluginNameField.text
        conf.mainClass = this.mainClassField.text
        conf.description = this.descriptionField.text
        conf.website = this.websiteField.text
        conf.velocityApiVersion = this.velocityApiVersionBox.selectedItem as String

        conf.setAuthors(this.authorsField.text)
        conf.setDependencies(this.dependField.text)
    }
}
