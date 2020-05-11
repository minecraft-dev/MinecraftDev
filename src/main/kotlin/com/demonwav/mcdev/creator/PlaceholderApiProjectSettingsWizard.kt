/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

@file:Suppress("Duplicates")

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.placeholderapi.PlaceholderApiProjectConfiguration
import com.demonwav.mcdev.util.firstOfType
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.apache.commons.lang.WordUtils

class PlaceholderApiProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {
    private lateinit var panel: JPanel
    private lateinit var expansionNameField: JTextField
    private lateinit var expansionVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var minecraftVersionBox: JComboBox<String>
    private lateinit var expansionAuthorField: JTextField

    private var config: PlaceholderApiProjectConfiguration? = null

    override fun getComponent(): JComponent {
        return panel
    }

    override fun updateStep() {
        config = creator.configs.firstOfType()
        if (config == null) {
            return
        }

        val buildSystem = creator.buildSystem ?: return

        val name = WordUtils.capitalize(buildSystem.artifactId.replace('-', ' '))
        expansionNameField.text = name
        expansionVersionField.text = buildSystem.version

        val conf = config ?: return

        if (creator.configs.indexOf(conf) != 0) {
            expansionNameField.isEditable = false
            expansionVersionField.isEditable = false
        }

        mainClassField.text = buildSystem.groupId.replace("-", "").toLowerCase() + "." +
            buildSystem.artifactId.replace("-", "").toLowerCase() + "." + name.replace(" ", "")

        CoroutineScope(Dispatchers.Swing).launch {
            try {
                withContext(Dispatchers.IO) { getVersionSelector(conf.type) }.set(minecraftVersionBox)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStepLeaving() {
        val conf = config ?: return
        conf.base = ProjectConfiguration.BaseConfigs(
            expansionNameField.text,
            expansionVersionField.text,
            mainClassField.text,
            expansionAuthorField.text
        )
    }

    override fun isStepVisible(): Boolean {
        return creator.configs.any { it is PlaceholderApiProjectConfiguration }
    }

    override fun updateDataModel() {}
}
