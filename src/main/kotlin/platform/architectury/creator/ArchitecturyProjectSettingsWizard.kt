/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.MinecraftModuleWizardStep
import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.creator.ValidatedField
import com.demonwav.mcdev.creator.ValidatedFieldType.CLASS_NAME
import com.demonwav.mcdev.creator.ValidatedFieldType.LIST
import com.demonwav.mcdev.creator.ValidatedFieldType.NON_BLANK
import com.demonwav.mcdev.platform.architectury.version.ArchitecturyVersion
import com.demonwav.mcdev.platform.architectury.version.FabricVersion
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.License
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.modUpdateStep
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.EnumComboBoxModel
import java.awt.event.ActionListener
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTextField
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

class ArchitecturyProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    @ValidatedField(NON_BLANK)
    private lateinit var modNameField: JTextField

    @ValidatedField(NON_BLANK, CLASS_NAME)
    private lateinit var panel: JPanel
    private lateinit var title: JLabel
    private lateinit var descriptionField: JTextField

    @ValidatedField(LIST)
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField
    private lateinit var repositoryField: JTextField
    private lateinit var issueField: JTextField
    private lateinit var licenseBox: JComboBox<License>
    private lateinit var mixinsCheckbox: JCheckBox
    private lateinit var architecturyCheckbox: JCheckBox
    private lateinit var fabricCheckbox: JCheckBox
    private lateinit var minecraftVersionBox: JComboBox<SemanticVersion>
    private lateinit var forgeVersionBox: JComboBox<SemanticVersion>
    private lateinit var fabricVersionBox: JComboBox<SemanticVersion>
    private lateinit var fabricApiVersionBox: JComboBox<SemanticVersion>
    private lateinit var architecturyApiVersionBox: JComboBox<SemanticVersion>
    private lateinit var loadingBar: JProgressBar
    private lateinit var minecraftVersionLabel: JLabel
    private lateinit var errorLabel: JLabel

    private var config: ArchitecturyProjectConfig? = null

    private data class ArchitecturyVersions(
        var fabricVersion: FabricVersion,
        var forgeVersion: ForgeVersion,
        var architecturyVersion: ArchitecturyVersion
    )

    private var versions: ArchitecturyVersions? = null

    private var currentJob: Job? = null

    private val forgeVersionBoxListener = ActionListener {
        val selectedVersion = forgeVersionBox.selectedItem as? SemanticVersion ?: return@ActionListener
        val supportedMixinVersion = selectedVersion >= SemanticVersion.release(31, 2, 45)

        mixinsCheckbox.isEnabled = supportedMixinVersion
        if (!supportedMixinVersion) {
            mixinsCheckbox.isSelected = false
        }
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

    override fun getComponent(): JComponent {
        return panel
    }

    override fun updateStep() {
        val (conf) = modUpdateStep<ArchitecturyProjectConfig>(creator, modNameField) ?: return
        config = conf

        title.icon = PlatformAssets.ARCHITECTURY_ICON_2X
        title.text = "<html><font size=\"5\">Architectury Settings</font></html>"

        minecraftVersionLabel.text = "Minecraft Version"

        licenseBox.model = EnumComboBoxModel(License::class.java)
        licenseBox.selectedItem = License.ALL_RIGHTS_RESERVED

        if (versions != null || currentJob?.isActive == true) {
            return
        }
        currentJob = updateVersions()
    }

    private fun setForgeVersion(data: Data) {
        forgeVersionBox.model = CollectionComboBoxModel(data.forgeVersions.subList(0, min(50, data.forgeVersions.size)))
        forgeVersionBox.selectedIndex = data.forgeSelectedIndex
    }

    private fun setFabricVersion(data: Data) {
        fabricVersionBox.model = CollectionComboBoxModel(
            data.fabricVersions.subList(0, min(50, data.fabricVersions.size))
        )
        fabricVersionBox.selectedIndex = data.fabricSelectedIndex
    }

    private fun setFabricApiVersion(data: Data) {
        fabricApiVersionBox.model = CollectionComboBoxModel(
            data.fabricApiVersions.subList(0, min(50, data.fabricApiVersions.size))
        )
        fabricApiVersionBox.selectedIndex = data.fabricApiSelectedIndex
    }

    private fun setArchitecturyApiVersion(data: Data) {
        architecturyApiVersionBox.model = CollectionComboBoxModel(
            data.architecturyApiVersions.subList(0, min(50, data.architecturyApiVersions.size))
        )
        architecturyApiVersionBox.selectedIndex = data.architecturyApiSelectedIndex
    }

    private val version: SemanticVersion?
        get() = minecraftVersionBox.selectedItem as? SemanticVersion

    override fun validate(): Boolean {
        return super.validate() && !loadingBar.isVisible
    }

    override fun isStepVisible(): Boolean {
        return creator.config is ArchitecturyProjectConfig
    }

    override fun onStepLeaving() {
        currentJob?.cancel()
    }

    override fun updateDataModel() {
        val conf = this.config ?: return

        conf.pluginName = this.modNameField.text
        conf.description = this.descriptionField.text
        conf.website = this.websiteField.text

        conf.modRepo = this.repositoryField.text
        conf.modIssue = this.issueField.text

        conf.setAuthors(this.authorsField.text)

        conf.mcVersion = this.version ?: SemanticVersion.release()

        (this.forgeVersionBox.selectedItem as SemanticVersion).let { version ->
            val versionString = version.toString()
            val forgeVersion = this.versions?.forgeVersion ?: return@let
            conf.forgeVersionText = forgeVersion.versions.first { it.endsWith(versionString) }
            conf.forgeVersion = version
        }
        (this.fabricVersionBox.selectedItem as SemanticVersion).let { version ->
            conf.fabricLoaderVersion = version
        }
        (this.fabricApiVersionBox.selectedItem as SemanticVersion).let { version ->
            conf.fabricApiVersion = version
        }
        (this.architecturyApiVersionBox.selectedItem as SemanticVersion).let { version ->
            conf.architecturyApiVersion = version
        }

        conf.fabricApi = fabricCheckbox.isSelected
        conf.architecturyApi = architecturyCheckbox.isSelected
        conf.mixins = mixinsCheckbox.isSelected
        conf.license = licenseBox.selectedItem as? License ?: License.ALL_RIGHTS_RESERVED
    }

    private fun mcVersionUpdate(data: Data) {
        forgeVersionBox.removeActionListener(forgeVersionBoxListener)
        setForgeVersion(data)
        forgeVersionBox.addActionListener(forgeVersionBoxListener)
        forgeVersionBoxListener.actionPerformed(null)

        fabricCheckbox.addActionListener {
            if (fabricCheckbox.isSelected) {
                architecturyCheckbox.isEnabled = true
            } else {
                architecturyCheckbox.isEnabled = false
                architecturyCheckbox.isSelected = false
            }
        }

        setFabricVersion(data)
        setFabricApiVersion(data)
        setArchitecturyApiVersion(data)
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
            LOGGER.error("Failed to update versions form", e)
            error()
        }

        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false

        currentJob = null
    }

    fun error() {
        errorLabel.isVisible = true
        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false
    }

    private suspend fun downloadVersions() = coroutineScope {
        val fabricVersionJob = asyncIO { FabricVersion.downloadData() }
        val forgeVersionJob = asyncIO { ForgeVersion.downloadData() }
        val architecturyApiVersionJob = asyncIO { ArchitecturyVersion.downloadData() }

        versions = ArchitecturyVersions(
            fabricVersionJob.await() ?: return@coroutineScope,
            forgeVersionJob.await() ?: return@coroutineScope,
            architecturyApiVersionJob.await() ?: return@coroutineScope
        )
    }

    private suspend fun updateForm(): Data? = coroutineScope {
        try {
            val vers = versions ?: return@coroutineScope null

            val selectedVersion = version ?: vers.forgeVersion.sortedMcVersions.firstOrNull()
                ?: return@coroutineScope null

            val fabricVersionsJob = asyncIO { vers.fabricVersion.getFabricVersions(selectedVersion) }
            val forgeVersionsJob = asyncIO { vers.forgeVersion.getForgeVersions(selectedVersion) }
            val fabricApiVersionsJob = asyncIO { vers.fabricVersion.getFabricApiVersions(selectedVersion) }
            val architecturyApiVersionsJob = asyncIO {
                vers.architecturyVersion.getArchitecturyVersions(selectedVersion)
            }

            // awaitAll is better than calling .await() individually
            val (
                fabricVersions,
                forgeVersions,
                fabricApiVersions,
                architecturyApiVersions,
            ) = listOf(
                fabricVersionsJob,
                forgeVersionsJob,
                fabricApiVersionsJob,
                architecturyApiVersionsJob
            ).awaitAll()

            val data = Data(0, fabricVersions, 0, forgeVersions, 0, fabricApiVersions, 0, architecturyApiVersions, 0)

            mcVersionUpdate(data)

            return@coroutineScope data
        } catch (e: Exception) {
            // log error manually - something is weird about intellij & coroutine exception handling
            LOGGER.error("Error while updating Architectury form version fields", e)
            return@coroutineScope null
        }
    }

    private fun updateMcForm(data: Data) {
        val vers = versions ?: return

        minecraftVersionBox.removeActionListener(minecraftBoxActionListener)
        minecraftVersionBox.removeAllItems()

        // make copy, so the next 2 operations don't mess up the map
        val mcVersions = vers.architecturyVersion.versions.keys.toCollection(LinkedHashSet())
        mcVersions.retainAll(vers.forgeVersion.sortedMcVersions.toSet())
        // Fabric also targets preview versions which aren't semver
        // The other option would be to try to parse all of them and catching any exceptions
        // But exceptions are slow, so this should be more efficient
        val fabricMcVersions = vers.fabricVersion.versions.minecraftVersions.mapTo(HashSet()) { it.name }
        mcVersions.retainAll { fabricMcVersions.contains(it.toString()) }

        minecraftVersionBox.model = CollectionComboBoxModel(mcVersions.sortedDescending())
        minecraftVersionBox.selectedIndex = data.mcSelectedIndex
        minecraftVersionBox.addActionListener(minecraftBoxActionListener)
    }

    private data class Data(
        val mcSelectedIndex: Int,
        val fabricVersions: List<SemanticVersion>,
        val fabricSelectedIndex: Int,
        val forgeVersions: List<SemanticVersion>,
        val forgeSelectedIndex: Int,
        val fabricApiVersions: List<SemanticVersion>,
        val fabricApiSelectedIndex: Int,
        val architecturyApiVersions: List<SemanticVersion>,
        val architecturyApiSelectedIndex: Int
    )

    companion object {
        val LOGGER = Logger.getInstance(ArchitecturyProjectSettingsWizard::class.java)
    }
}
