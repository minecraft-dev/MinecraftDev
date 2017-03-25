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

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration
import com.demonwav.mcdev.platform.mcp.version.McpVersion
import com.demonwav.mcdev.platform.mcp.version.McpVersionEntry
import org.apache.commons.lang.WordUtils
import java.awt.event.ActionListener
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTextField
import javax.swing.SwingWorker

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

    private var settings: ForgeProjectConfiguration? = null

    private var mcpVersion: McpVersion? = null
    private var forgeVersion: ForgeVersion? = null

    private val mcpBoxActionListener = ActionListener {
        mcpWarning.isVisible = (mcpVersionBox.selectedItem as McpVersionEntry).isRed
    }

    private var apiWorker = ForgeWorker()

    init {
        generateDocsCheckbox.isVisible = false
        mcpWarning.isVisible = false

        minecraftVersionBox.addActionListener {
            if (mcpVersion != null) {
                mcpVersion!!.setMcpVersion(mcpVersionBox, version!!, mcpBoxActionListener)
            }
            setForgeVersion()
        }
    }

    override fun getComponent(): JComponent? {
        settings = creator.settings[PlatformType.FORGE] as? ForgeProjectConfiguration
        if (settings == null) {
            return null
        }

        pluginNameField.text = WordUtils.capitalize(creator.artifactId)
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

        if (settings is SpongeForgeProjectConfiguration) {
            title.icon = PlatformAssets.SPONGE_FORGE_ICON_2X
            title.text = "<html><font size=\"5\">SpongeForge Settings</font></html>"
            generateDocsCheckbox.isVisible = true

            minecraftVersionLabel.text = "    Sponge API"
        }

        apiWorker.execute()

        return panel
    }

    override fun updateStep() {
        if ((forgeVersion == null || mcpVersion == null) && (apiWorker.isCancelled || apiWorker.isDone)) {
            // A SwingWorker will only run once, so we need to create a new instance
            apiWorker = ForgeWorker()
            apiWorker.execute()
        }
    }

    private fun setForgeVersion() {
        if (forgeVersion == null) {
            return
        }

        val version = version ?: return

        forgeVersionBox.removeAllItems()
        val versions = forgeVersion!!.getForgeVersions(version) ?: return

        versions.stream().sorted { one, two -> one.compareTo(two) * -1 }.forEach { forgeVersionBox.addItem(it) }

        val promo = forgeVersion!!.getPromo(version)
        if (promo != null) {
            var index = 0
            for (i in 0..forgeVersionBox.itemCount - 1) {
                try {
                    if (forgeVersionBox.getItemAt(i).endsWith(promo.toInt().toString())) {
                        index = i
                    }
                } catch (ignored: NumberFormatException) {
                }

            }
            forgeVersionBox.selectedIndex = index
        }
    }

    private val version: String?
        get() {
            val version: String
            if (settings !is SpongeForgeProjectConfiguration) {
                version = minecraftVersionBox.selectedItem as String
            } else {
                if (minecraftVersionBox.selectedItem == "4.1.0") {
                    version = "1.8.9"
                } else {
                    version = "1.10.2"
                }
            }
            return version
        }

    override fun validate(): Boolean {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField, MinecraftModuleWizardStep.pattern) && !loadingBar.isVisible
    }

    override fun isStepVisible(): Boolean {
        settings = creator.settings[PlatformType.FORGE] as? ForgeProjectConfiguration
        return settings != null
    }

    override fun onStepLeaving() {
        if (loadingBar.isVisible) {
            // we're in a cancel state
            apiWorker.cancel(true)
            return
        }

        settings!!.pluginName = pluginNameField.text
        settings!!.pluginVersion = pluginVersionField.text
        settings!!.mainClass = mainClassField.text

        settings!!.setAuthors(authorsField.text)
        settings!!.setDependencies(dependField.text)
        settings!!.description = descriptionField.text
        settings!!.website = websiteField.text
        settings!!.updateUrl = updateUrlField.text

        settings!!.mcpVersion = (mcpVersionBox.selectedItem as McpVersionEntry).text

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

    override fun updateDataModel() {}

    private inner class ForgeWorker : SwingWorker<Any, Any>() {
        override fun doInBackground(): Any? {
            mcpVersion = McpVersion.downloadData()
            forgeVersion = ForgeVersion.downloadData()
            return null
        }

        override fun done() {
            if (mcpVersion == null) {
                return
            }

            minecraftVersionBox.removeAllItems()

            // reverse order the versions
            if (settings !is SpongeForgeProjectConfiguration) {
                forgeVersion?.sortedMcVersions?.forEach { minecraftVersionBox.addItem(it) }
                val recommended = forgeVersion!!.getRecommended(mcpVersion!!.versions)

                var index = 0
                for (i in 0..minecraftVersionBox.itemCount - 1) {
                    if (minecraftVersionBox.getItemAt(i) == recommended) {
                        index = i
                    }
                }
                minecraftVersionBox.setSelectedIndex(index)
            } else {
                minecraftVersionBox.addItem("4.1.0")
                minecraftVersionBox.addItem("5.0.0")
                minecraftVersionBox.setSelectedIndex(1)
            }

            if (mcpVersion != null) {
                mcpVersion!!.setMcpVersion(mcpVersionBox, version!!, mcpBoxActionListener)
            }

            if (forgeVersion == null) {
                return
            }

            setForgeVersion()

            loadingBar.isIndeterminate = false
            loadingBar.isVisible = false
        }
    }
}
