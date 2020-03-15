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
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
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
import org.apache.commons.lang.WordUtils

class SpongeProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var pluginNameField: JTextField
    private lateinit var pluginVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var panel: JPanel
    private lateinit var title: JLabel
    private lateinit var descriptionField: JTextField
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField
    private lateinit var dependField: JTextField
    private lateinit var spongeApiVersionBox: JComboBox<String>
    private lateinit var errorLabel: JLabel

    private var config: SpongeProjectConfiguration? = null

    override fun getComponent(): JComponent {
        return panel
    }

    override fun validate(): Boolean {
        return validate(
            pluginNameField,
            pluginVersionField,
            mainClassField,
            authorsField,
            dependField,
            MinecraftModuleWizardStep.pattern
        ) &&
            spongeApiVersionBox.selectedItem != null
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
            mainClassField.text = mainClassField.text + PlatformType.SPONGE.normalName
        }

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
        return creator.configs.any { it is SpongeProjectConfiguration }
    }

    override fun onStepLeaving() {
        val conf = config ?: return
        conf.base = ProjectConfiguration.BaseConfigs(
            pluginNameField.text,
            pluginVersionField.text,
            mainClassField.text,
            descriptionField.text,
            websiteField.text
        )

        conf.setAuthors(authorsField.text)
        conf.setDependencies(dependField.text)

        conf.spongeApiVersion = spongeApiVersionBox.selectedItem as? String ?: ""
    }

    override fun updateDataModel() {}
}
