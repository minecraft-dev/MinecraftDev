/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.exception.MinecraftSetupException
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
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTextField
import javax.swing.SwingWorker
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint

class ForgeProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var pluginNameField: JTextField
    private lateinit var pluginVersionField: JTextField
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
    private lateinit var generateDocsCheckbox: JCheckBox
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
        }.done { data ->
            invokeLaterAny {
                mcVersionUpdate(data)
            }
        }
    }

    private var apiWorker = ForgeWorker(null)

    init {
        generateDocsCheckbox.isVisible = false
        mcpWarning.isVisible = false
    }

    override fun getComponent(): JComponent? {
        settings = creator.settings[PlatformType.FORGE] as? ForgeProjectConfiguration
        if (settings == null) {
            return null
        }

        pluginNameField.text = creator.artifactId.toLowerCase()
        pluginVersionField.text = creator.version

        if (settings != null && !settings!!.isFirst) {
            pluginNameField.isEditable = false
            pluginVersionField.isEditable = false
        }

        mainClassField.text = "${this.creator.groupId.toLowerCase()}.${this.creator.artifactId.toLowerCase()}." +
            WordUtils.capitalize(this.creator.artifactId)

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
            generateDocsCheckbox.isVisible = true

            minecraftVersionLabel.text = "Sponge API Version"
        } else {
            title.icon = PlatformAssets.FORGE_ICON_2X
            title.text = "<html><font size=\"5\">Forge Settings</font></html>"
            generateDocsCheckbox.isVisible = false

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
            }.done { data ->
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
            if (isSpongeForge == true || (isSpongeForge == null && settings is SpongeForgeProjectConfiguration)) {
                return spongeVersion?.let { it.versions[minecraftVersionBox.selectedItem as? String] }
            } else {
                return minecraftVersionBox.selectedItem as? String
            }
        }

    override fun validate(): Boolean {
        if (validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField, MinecraftModuleWizardStep.pattern) && !loadingBar.isVisible) {
            try {
                if (pluginNameField.text.toLowerCase() != pluginNameField.text) {
                    throw MinecraftSetupException("Forge modids need to be lowercase only", pluginNameField)
                }
            } catch (e: MinecraftSetupException) {
                val message = e.error
                JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                        .setFadeoutTime(4000)
                        .createBalloon()
                        .show(RelativePoint.getSouthWestOf(e.j), Balloon.Position.below)
                return false
            }
        }
        return true
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
            pluginName = pluginNameField.text
            pluginVersion = pluginVersionField.text
            mainClass = mainClassField.text

            setAuthors(authorsField.text)
            setDependencies(dependField.text)
            description = descriptionField.text
            website = websiteField.text
            updateUrl = updateUrlField.text

            mcpVersion = (mcpVersionBox.selectedItem as McpVersionEntry).text
        }

        if (settings is SpongeForgeProjectConfiguration) {
            val configuration = settings as SpongeForgeProjectConfiguration?
            configuration!!.generateDocumentation = generateDocsCheckbox.isSelected
            configuration.spongeApiVersion = minecraftVersionBox.selectedItem as String
        }

        // If an error occurs while fetching the API, this may prevent the user from closing the dialog.
        val fullVersion = forgeVersion?.getFullVersion(forgeVersionBox.selectedItem as String)
        if (fullVersion != null) {
            settings!!.forgeVersion = fullVersion
        }
    }

    fun error() {
        errorLabel.isVisible = true
        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false
    }

    override fun updateDataModel() {}

    private fun getData(version: String?): Data {
        val recommended = forgeVersion!!.getRecommended(mcpVersion!!.versions)

        var index = forgeVersion!!.sortedMcVersions.indexOf(recommended)
        if (index == -1) {
            index = 0
        }

        var finalVersion = version ?: if (settings !is SpongeForgeProjectConfiguration) {
            forgeVersion!!.sortedMcVersions[index]
        } else {
            spongeVersion!!.versions.values.toTypedArray()[spongeVersion!!.selectedIndex]
        }

        if (settings !is SpongeForgeProjectConfiguration) {
            index = forgeVersion!!.sortedMcVersions.indexOf(finalVersion)
        } else {
            if (spongeVersion!!.versions.containsValue(finalVersion)) {
                index = -1
                var i = 0
                for ((_, value) in spongeVersion!!.versions) {
                    i++
                    if (value == finalVersion) {
                        index = i
                        break
                    }
                }
            } else {
                finalVersion = spongeVersion!!.versions.values.toTypedArray()[spongeVersion!!.selectedIndex]
                index = forgeVersion!!.sortedMcVersions.indexOf(finalVersion)
            }
        }

        val mcpVersionList = mcpVersion!!.getMcpVersionList(finalVersion)
        val forgeVersions = forgeVersion!!.getForgeVersions(finalVersion).sortedWith(Comparator.reverseOrder())

        var forgeIndex = 0
        val promo = forgeVersion!!.getPromo(finalVersion)
        if (promo != null) {
            for (i in 0 until forgeVersions.size) {
                try {
                    if (forgeVersions[i].endsWith(promo.toInt().toString())) {
                        forgeIndex = i
                        break
                    }
                } catch (ignored: NumberFormatException) {}
            }
        }

        return Data(index, mcpVersionList, forgeVersions, forgeIndex)
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
