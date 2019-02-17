/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration
import com.demonwav.mcdev.platform.mcp.version.McpVersion
import com.demonwav.mcdev.platform.mcp.version.McpVersionEntry
import com.demonwav.mcdev.platform.sponge.SpongeVersion
import com.demonwav.mcdev.util.invokeLaterAny
import com.intellij.util.ui.UIUtil
import org.apache.commons.lang.WordUtils
import org.jetbrains.concurrency.runAsync
import java.awt.event.ActionListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTextField
import javax.swing.SwingWorker

class ForgeProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var modNameField: JTextField
    private lateinit var modVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var panel: JPanel
    private lateinit var title: JLabel
    private lateinit var descriptionField: JTextField
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField
    private lateinit var dependField: JTextField
    private lateinit var updateUrlField: JTextField
    private lateinit var minecraftVersionBox: JComboBox<String>
    private lateinit var forgeVersionBox: JComboBox<String>
    private lateinit var mcpVersionBox: JComboBox<McpVersionEntry>
    private lateinit var loadingBar: JProgressBar
    private lateinit var minecraftVersionLabel: JLabel
    private lateinit var mcpWarning: JLabel
    private lateinit var errorLabel: JLabel

    private var settings: ForgeProjectConfiguration? = null
    private var isSpongeForge: Boolean? = null

    private var spongeVersion: SpongeVersion? = null
    private var mcpVersion: McpVersion? = null
    private var forgeVersion: ForgeVersion? = null

    private val mcpBoxActionListener = ActionListener {
        mcpWarning.isVisible = (mcpVersionBox.selectedItem as? McpVersionEntry)?.isRed == true
    }

    private val minecraftBoxActionListener = ActionListener {
        val tempVersion = version
        runAsync {
            getData(tempVersion)
        }.onSuccess { data ->
            invokeLaterAny {
                mcVersionUpdate(data)
            }
        }
    }

    private var apiWorker = ForgeWorker(null)

    init {
        mcpWarning.isVisible = false
    }

    override fun getComponent(): JComponent? {
        settings = creator.settings[PlatformType.FORGE] as? ForgeProjectConfiguration
        if (settings == null) {
            return null
        }

        modNameField.text = WordUtils.capitalize(creator.artifactId.replace('-', ' '))
        modVersionField.text = creator.version

        if (settings != null && !settings!!.isFirst) {
            modNameField.isEditable = false
            modVersionField.isEditable = false
        }

        mainClassField.text = this.creator.groupId.replace("-", "").toLowerCase() + "." +
            this.creator.artifactId.replace("-", "").toLowerCase() + "." +
            WordUtils.capitalize(this.creator.artifactId.replace('-', ' ')).replace(" ", "")

        if (creator.settings.size > 1) {
            mainClassField.text = mainClassField.text + PlatformType.FORGE.normalName
        }

        loadingBar.isIndeterminate = true

        apiWorker.execute()

        return panel
    }

    override fun updateStep() {
        if (settings is SpongeForgeProjectConfiguration) {
            if (UIUtil.isUnderDarcula()) {
                title.icon = PlatformAssets.SPONGE_FORGE_ICON_2X_DARK
            } else {
                title.icon = PlatformAssets.SPONGE_ICON_2X_DARK
            }
            title.text = "<html><font size=\"5\">SpongeForge Settings</font></html>"

            minecraftVersionLabel.text = "Sponge API Version"
        } else {
            title.icon = PlatformAssets.FORGE_ICON_2X
            title.text = "<html><font size=\"5\">Forge Settings</font></html>"

            minecraftVersionLabel.text = "Minecraft Version"
        }

        if ((forgeVersion == null || mcpVersion == null || spongeVersion == null) && (apiWorker.isCancelled || apiWorker.isDone)) {
            // A SwingWorker will only run once, so we need to create a new instance
            apiWorker = ForgeWorker(version)
            apiWorker.execute()
        } else if (forgeVersion != null && mcpVersion != null && spongeVersion != null) {
            // always make sure versions are reset in case of a state change
            val tempVersion = version
            runAsync {
                getData(tempVersion)
            }.onSuccess { data ->
                invokeLaterAny {
                    update(data)
                }
            }
        }
    }

    private fun setForgeVersion(data: Data) {
        forgeVersionBox.model = DefaultComboBoxModel<String>(data.forgeVersions.toTypedArray())
        forgeVersionBox.selectedIndex = data.forgeSelectedIndex
    }

    private val version: String?
        get() {
            return if (isSpongeForge == true || (isSpongeForge == null && settings is SpongeForgeProjectConfiguration)) {
                spongeVersion?.let { it.versions[minecraftVersionBox.selectedItem as? String] }
            } else {
                minecraftVersionBox.selectedItem as? String
            }
        }

    override fun validate(): Boolean {
        return validate(modNameField, modVersionField, mainClassField, authorsField, dependField, MinecraftModuleWizardStep.pattern) && !loadingBar.isVisible
    }

    override fun isStepVisible(): Boolean {
        settings = creator.settings[PlatformType.FORGE] as? ForgeProjectConfiguration
        return settings != null
    }

    override fun onStepLeaving() {
        if (loadingBar.isVisible || errorLabel.isVisible) {
            // we're in a cancel state
            apiWorker.cancel(true)
            return
        }

        settings!!.apply {
            pluginName = modNameField.text
            pluginVersion = modVersionField.text
            mainClass = mainClassField.text

            setAuthors(authorsField.text)
            setDependencies(dependField.text)
            description = descriptionField.text
            website = websiteField.text
            updateUrl = updateUrlField.text

            mcpVersion = (mcpVersionBox.selectedItem as McpVersionEntry).text
        }

        (settings as? SpongeForgeProjectConfiguration)?.let { settings ->
            settings.spongeApiVersion = minecraftVersionBox.selectedItem as String
        }

        (forgeVersionBox.selectedItem as? String)?.let { version ->
            settings!!.forgeVersion = forgeVersion!!.versions.first { it.endsWith(version) }
        }
        settings!!.mcVersion = version!!
    }

    fun error() {
        errorLabel.isVisible = true
        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false
    }

    override fun updateDataModel() {}

    private fun getData(version: String?): Data {
        val finalVersion = when {
            version != null -> version
            settings !is SpongeForgeProjectConfiguration -> {
                forgeVersion!!.sortedMcVersions[0]
            }
            else -> {
                val versionString = spongeVersion!!.versions.values.toTypedArray()[spongeVersion!!.selectedIndex]
                forgeVersion!!.sortedMcVersions.first { it == versionString }
            }
        }

        val mcIndex = when {
            settings is SpongeForgeProjectConfiguration && spongeVersion!!.versions.containsValue(finalVersion) -> {
                var i = 0
                for ((_, value) in spongeVersion!!.versions) {
                    i++
                    if (value == finalVersion) {
                        break
                    }
                }
                i
            }
            else -> {
                0
            }
        }

        val mcpVersionList = mcpVersion!!.getMcpVersionList(finalVersion)
        val forgeVersions = forgeVersion!!.getForgeVersions(finalVersion)
        forgeVersions.sortDescending()

        for (i in 0 until forgeVersions.size) {
            forgeVersions[i] = forgeVersions[i].substring(forgeVersions[i].indexOf('-') + 1)
        }

        val forgeIndex = 0

        return Data(mcIndex, mcpVersionList, forgeVersions, forgeIndex)
    }

    private fun update(data: Data) {
        if (spongeVersion == null || mcpVersion == null || forgeVersion == null) {
            error()
            return
        }

        minecraftVersionBox.removeActionListener(minecraftBoxActionListener)
        minecraftVersionBox.removeAllItems()

        if (settings !is SpongeForgeProjectConfiguration) {
            forgeVersion?.sortedMcVersions?.forEach { minecraftVersionBox.addItem(it) }
            minecraftVersionBox.setSelectedIndex(data.mcSelectedIndex)
        } else {
            spongeVersion!!.set(minecraftVersionBox)
        }
        minecraftVersionBox.addActionListener(minecraftBoxActionListener)

        mcVersionUpdate(data)

        // if we come back to this step we need to remember our "past" state (it may change)
        isSpongeForge = settings is SpongeForgeProjectConfiguration

        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false
    }

    private fun mcVersionUpdate(data: Data) {
        mcpVersionBox.removeActionListener(mcpBoxActionListener)
        mcpVersionBox.model = DefaultComboBoxModel<McpVersionEntry>(data.mcpVersions.toTypedArray())
        mcpVersionBox.selectedIndex = 0
        mcpVersionBox.addActionListener(mcpBoxActionListener)
        mcpBoxActionListener.actionPerformed(null)

        setForgeVersion(data)
    }

    private inner class ForgeWorker(val version: String?) : SwingWorker<Data, Any>() {
        override fun doInBackground(): Data {
            spongeVersion = SpongeVersion.downloadData()
            mcpVersion = McpVersion.downloadData()
            forgeVersion = ForgeVersion.downloadData()
            return getData(version)
        }

        public override fun done() {
            if (spongeVersion == null || mcpVersion == null || forgeVersion == null) {
                error()
                return
            }

            update(get())
        }
    }

    private data class Data(
        val mcSelectedIndex: Int,
        val mcpVersions: List<McpVersionEntry>,
        val forgeVersions: List<String>,
        val forgeSelectedIndex: Int
    )
}
