/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
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
import com.demonwav.mcdev.util.firstOfType
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

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

    override fun getComponent(): JComponent {
        return panel
    }

    override fun validate(): Boolean {
        return super.validate() && velocityApiVersionBox.selectedItem != null
    }

    override fun updateStep() {
        config = creator.configs.firstOfType()
        if (config == null) {
            return
        }
        val conf = config ?: return

        basicUpdateStep(creator, conf, pluginNameField, mainClassField)

        velocityApiVersionBox.addItem("1.0.0-SNAPSHOT")
        // CoroutineScope(Dispatchers.Swing).launch {
        //     try {
        //         TODO withContext(Dispatchers.IO) { SpongeVersion.downloadData() }?.set(velocityApiVersionBox)
        //     } catch (e: Exception) {
        //         errorLabel.isVisible = true
        //     }
        // }
    }

    override fun isStepVisible(): Boolean {
        return creator.configs.any { it is VelocityProjectConfig }
    }

    override fun onStepLeaving() {
        val conf = this.config ?: return

        conf.pluginName = this.pluginNameField.text
        conf.mainClass = this.mainClassField.text
        conf.description = this.descriptionField.text
        conf.website = this.websiteField.text
        conf.velocityApiVersion = this.velocityApiVersionBox.selectedItem as String

        conf.setAuthors(this.authorsField.text)
        // conf.setDependencies(this.dependField.text) TODO
    }

    override fun updateDataModel() {}
}
