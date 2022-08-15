/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
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
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.util.License
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ui.EnumComboBoxModel
import com.intellij.util.text.nullize
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
    private lateinit var licenseBox: JComboBox<License>
    private lateinit var spongeApiVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var config: SpongeProjectConfig? = null

    private var versionsLoaded: Boolean = false

    init {
        spongeApiVersionBox.addActionListener {
            val stringVersion = spongeApiVersionBox.selectedItem as? String
            licenseBox.isEnabled = stringVersion == null || SemanticVersion.parse(stringVersion) >= SpongeConstants.API8
        }
    }

    override fun getComponent(): JComponent {
        return panel
    }

    override fun validate(): Boolean {
        return super.validate() && spongeApiVersionBox.selectedItem != null
    }

    override fun updateStep() {
        config = creator.config as? SpongeProjectConfig
        if (config == null) {
            return
        }

        basicUpdateStep(creator, pluginNameField, mainClassField)

        if (UIUtil.isUnderDarcula()) {
            title.icon = PlatformAssets.SPONGE_ICON_2X_DARK
        } else {
            title.icon = PlatformAssets.SPONGE_ICON_2X
        }

        licenseBox.model = EnumComboBoxModel(License::class.java)
        licenseBox.selectedItem = License.ALL_RIGHTS_RESERVED

        if (versionsLoaded) {
            return
        }

        versionsLoaded = true
        CoroutineScope(Dispatchers.Swing).launch {
            try {
                withContext(Dispatchers.IO) { SpongeVersion.downloadData() }?.set(spongeApiVersionBox)
            } catch (e: Exception) {
                errorLabel.isVisible = true
            }
        }
    }

    override fun isStepVisible(): Boolean {
        return creator.config is SpongeProjectConfig
    }

    override fun updateDataModel() {
        val conf = this.config ?: return

        conf.pluginName = this.pluginNameField.text
        conf.mainClass = this.mainClassField.text
        conf.description = this.descriptionField.text?.nullize(true)
        conf.website = this.websiteField.text?.nullize(true)
        conf.spongeApiVersion = this.spongeApiVersionBox.selectedItem as String

        conf.setAuthors(this.authorsField.text)
        conf.setDependencies(this.dependField.text)
        conf.license = this.licenseBox.selectedItem as License
    }
}
