/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.platform.mcp.version.McpVersion
import com.demonwav.mcdev.platform.mcp.version.McpVersionEntry
import com.demonwav.mcdev.util.firstOfType
import com.intellij.ui.CollectionComboBoxModel
import java.awt.event.ActionListener
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.apache.commons.lang.WordUtils

class ForgeProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var modNameField: JTextField
    private lateinit var modVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var panel: JPanel
    private lateinit var title: JLabel
    private lateinit var descriptionField: JTextField
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField
    private lateinit var updateUrlField: JTextField
    private lateinit var minecraftVersionBox: JComboBox<String>
    private lateinit var forgeVersionBox: JComboBox<String>
    private lateinit var mcpVersionBox: JComboBox<McpVersionEntry>
    private lateinit var loadingBar: JProgressBar
    private lateinit var minecraftVersionLabel: JLabel
    private lateinit var mcpWarning: JLabel
    private lateinit var errorLabel: JLabel

    private var config: ForgeProjectConfiguration? = null

    private data class ForgeVersions(
        var mcpVersion: McpVersion,
        var forgeVersion: ForgeVersion
    )

    private var versions: ForgeVersions? = null

    private var currentJob: Job? = null

    private val mcpBoxActionListener = ActionListener {
        mcpWarning.isVisible = (mcpVersionBox.selectedItem as? McpVersionEntry)?.isRed == true
    }

    private val minecraftBoxActionListener: ActionListener = ActionListener {
        CoroutineScope(Dispatchers.Swing).launch {
            loadingBar.isIndeterminate = true
            loadingBar.isVisible = true

            updateForm()

            loadingBar.isIndeterminate = false
            loadingBar.isVisible = false
        }
    }

    init {
        mcpWarning.isVisible = false
    }

    override fun getComponent(): JComponent {
        return panel
    }

    override fun updateStep() {
        config = creator.configs.firstOfType()

        val buildSystem = creator.buildSystem ?: return

        modNameField.text = WordUtils.capitalize(buildSystem.artifactId.replace('-', ' '))
        modVersionField.text = buildSystem.version

        val conf = config ?: return

        if (creator.configs.indexOf(conf) != 0) {
            modNameField.isEditable = false
            modVersionField.isEditable = false
        }

        mainClassField.text = buildSystem.groupId.replace("-", "").toLowerCase() + "." +
            buildSystem.artifactId.replace("-", "").toLowerCase() + "." +
            WordUtils.capitalize(buildSystem.artifactId.replace('-', ' ')).replace(" ", "")

        if (creator.configs.size > 1) {
            mainClassField.text = mainClassField.text + PlatformType.FORGE.normalName
        }

        title.icon = PlatformAssets.FORGE_ICON_2X
        title.text = "<html><font size=\"5\">Forge Settings</font></html>"

        minecraftVersionLabel.text = "Minecraft Version"

        if (versions != null || currentJob?.isActive == true) {
            return
        }
        currentJob = updateVersions()
    }

    private fun setForgeVersion(data: Data) {
        forgeVersionBox.model = CollectionComboBoxModel(data.forgeVersions)
        forgeVersionBox.selectedIndex = data.forgeSelectedIndex
    }

    private val version: String?
        get() = minecraftVersionBox.selectedItem as? String

    override fun validate(): Boolean {
        return validate(
            modNameField,
            modVersionField,
            mainClassField,
            authorsField,
            null,
            pattern
        ) && !loadingBar.isVisible
    }

    override fun isStepVisible(): Boolean {
        return creator.configs.any { it is ForgeProjectConfiguration }
    }

    override fun onStepLeaving() {
        currentJob?.let { job ->
            // we're in a cancel state
            job.cancel()
            return
        }

        val conf = config ?: return
        conf.base = ProjectConfiguration.BaseConfigs(
            pluginName = modNameField.text,
            pluginVersion = modVersionField.text,
            mainClass = mainClassField.text,
            description = descriptionField.text,
            website = websiteField.text
        )

        conf.setAuthors(authorsField.text)
        conf.updateUrl = updateUrlField.text

        conf.mcpVersion = (mcpVersionBox.selectedItem as McpVersionEntry).versionPair

        (forgeVersionBox.selectedItem as? String)?.let { version ->
            val forgeVersion = versions?.forgeVersion ?: return@let
            conf.forgeVersion = forgeVersion.versions.first { it.endsWith(version) }
        }

        conf.mcVersion = version ?: ""
    }

    fun error() {
        errorLabel.isVisible = true
        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false
    }

    override fun updateDataModel() {}

    private fun mcVersionUpdate(data: Data) {
        mcpVersionBox.removeActionListener(mcpBoxActionListener)
        mcpVersionBox.model = CollectionComboBoxModel(data.mcpVersions)
        mcpVersionBox.selectedIndex = 0
        mcpVersionBox.addActionListener(mcpBoxActionListener)
        mcpBoxActionListener.actionPerformed(null)

        setForgeVersion(data)
    }

    private fun updateVersions() = CoroutineScope(Dispatchers.Swing).launch {
        loadingBar.isIndeterminate = true
        loadingBar.isVisible = true

        try {
            downloadVersions()
            val data = updateForm()
            if (data != null) {
                updateMcForm(data)
            }
        } catch (e: Exception) {
            error()
        }

        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false

        currentJob = null
    }

    private suspend fun downloadVersions() = coroutineScope {
        val mcpVersionJob = async(Dispatchers.IO) { McpVersion.downloadData() }
        val forgeVersionJob = async(Dispatchers.IO) { ForgeVersion.downloadData() }

        versions = ForgeVersions(
            mcpVersionJob.await() ?: return@coroutineScope,
            forgeVersionJob.await() ?: return@coroutineScope
        )
    }

    private suspend fun updateForm(): Data? = coroutineScope {
        val vers = versions ?: return@coroutineScope null

        val finalVersion = when {
            version != null -> {
                // selected version
                version ?: return@coroutineScope null
            }
            else -> {
                // Default Forge
                vers.forgeVersion.sortedMcVersions.firstOrNull() ?: return@coroutineScope null
            }
        }

        val mcpVersionListJob = async(Dispatchers.IO) { vers.mcpVersion.getMcpVersionList(finalVersion) }
        val forgeVersionsJob = async(Dispatchers.IO) {
            val list = vers.forgeVersion.getForgeVersions(finalVersion)
            list.sortDescending()
            for (i in 0 until list.size) {
                list[i] = list[i].substring(list[i].indexOf('-') + 1)
            }
            return@async list
        }

        val mcpVersionList = mcpVersionListJob.await()
        val forgeVersions = forgeVersionsJob.await()

        val forgeIndex = 0

        val data = Data(0, mcpVersionList, forgeVersions, forgeIndex)

        mcVersionUpdate(data)

        return@coroutineScope data
    }

    private fun updateMcForm(data: Data) {
        val vers = versions ?: return

        minecraftVersionBox.removeActionListener(minecraftBoxActionListener)
        minecraftVersionBox.removeAllItems()

        minecraftVersionBox.model = CollectionComboBoxModel(vers.forgeVersion.sortedMcVersions)
        minecraftVersionBox.selectedIndex = data.mcSelectedIndex
        minecraftVersionBox.addActionListener(minecraftBoxActionListener)
    }

    private data class Data(
        val mcSelectedIndex: Int,
        val mcpVersions: List<McpVersionEntry>,
        val forgeVersions: List<String>,
        val forgeSelectedIndex: Int
    )
}
