/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.MinecraftModuleWizardStep
import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.creator.ValidatedField
import com.demonwav.mcdev.creator.ValidatedFieldType.CLASS_NAME
import com.demonwav.mcdev.creator.ValidatedFieldType.LIST
import com.demonwav.mcdev.creator.ValidatedFieldType.NON_BLANK
import com.demonwav.mcdev.platform.sponge.SpongeVersion
import com.demonwav.mcdev.util.firstOfType
import com.intellij.util.ui.UIUtil
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

class SpongeProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

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
    private lateinit var spongeApiVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var config: SpongeProjectConfig? = null

    override fun getComponent(): JComponent {
        return panel
    }

    override fun validate(): Boolean {
        return super.validate() && spongeApiVersionBox.selectedItem != null
    }

    override fun updateStep() {
        config = creator.configs.firstOfType()
        if (config == null) {
            return
        }
        val conf = config ?: return

        basicUpdateStep(creator, conf, pluginNameField, mainClassField)

        if (UIUtil.isUnderDarcula()) {
            title.icon = PlatformAssets.SPONGE_ICON_2X_DARK
        } else {
            title.icon = PlatformAssets.SPONGE_ICON_2X
        }

        CoroutineScope(Dispatchers.Swing).launch {
            try {
                withContext(Dispatchers.IO) { SpongeVersion.downloadData() }?.set(spongeApiVersionBox)
            } catch (e: Exception) {
                errorLabel.isVisible = true
            }
        }
    }

    override fun isStepVisible(): Boolean {
        return creator.configs.any { it is SpongeProjectConfig }
    }

    override fun onStepLeaving() {
        val conf = this.config ?: return

        conf.pluginName = this.pluginNameField.text
        conf.mainClass = this.mainClassField.text
        conf.description = this.descriptionField.text
        conf.website = this.websiteField.text
        conf.spongeApiVersion = this.spongeApiVersionBox.selectedItem as String

        conf.setAuthors(this.authorsField.text)
        conf.setDependencies(this.dependField.text)
    }

    override fun updateDataModel() {}
}
