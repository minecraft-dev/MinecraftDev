/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
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
import com.demonwav.mcdev.util.modUpdateStep
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
import kotlinx.coroutines.async
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
        val (conf, buildSystem) = modUpdateStep<ArchitecturyProjectConfig>(creator, modNameField) ?: return
        config = conf

        if (creator.configs.indexOf(conf) != 0) {
            modNameField.isEditable = false
        }

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
        return creator.configs.any { it is ArchitecturyProjectConfig }
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
        conf.modRepo = this.issueField.text

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

        conf.mixins = mixinsCheckbox.isSelected
        conf.license = licenseBox.selectedItem as? License ?: License.ALL_RIGHTS_RESERVED
    }

    private fun mcVersionUpdate(data: Data) {
        forgeVersionBox.removeActionListener(forgeVersionBoxListener)
        setForgeVersion(data)
        forgeVersionBox.addActionListener(forgeVersionBoxListener)
        forgeVersionBoxListener.actionPerformed(null)

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
        val fabricVersionJob = async(Dispatchers.IO) { FabricVersion.downloadData() }
        val forgeVersionJob = async(Dispatchers.IO) { ForgeVersion.downloadData() }
        val architecturyApiVersionJob = async(Dispatchers.IO) { ArchitecturyVersion.downloadData() }

        versions = ArchitecturyVersions(
            fabricVersionJob.await() ?: return@coroutineScope,
            forgeVersionJob.await() ?: return@coroutineScope,
            architecturyApiVersionJob.await() ?: return@coroutineScope
        )
    }

    private suspend fun updateForm(): Data? = coroutineScope {
        val vers = versions ?: return@coroutineScope null

        val selectedVersion = version ?: vers.forgeVersion.sortedMcVersions.firstOrNull() ?: return@coroutineScope null

        val fabricVersionsJob = async(Dispatchers.IO) { vers.fabricVersion.getFabricVersions(selectedVersion) }
        val forgeVersionsJob = async(Dispatchers.IO) { vers.forgeVersion.getForgeVersions(selectedVersion) }
        val fabricApiVersionsJob = async(Dispatchers.IO) { vers.fabricVersion.getFabricApiVersions(selectedVersion) }
        val architecturyApiVersionsJob = async(Dispatchers.IO) {
            vers.architecturyVersion.getArchitecturyVersions(
                selectedVersion
            )
        }

        val fabricVersions = fabricVersionsJob.await()
        val forgeVersions = forgeVersionsJob.await()
        val fabricApiVersions = fabricApiVersionsJob.await()
        val architecturyApiVersions = architecturyApiVersionsJob.await()

        val data = Data(0, fabricVersions, 0, forgeVersions, 0, fabricApiVersions, 0, architecturyApiVersions, 0)

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
        val fabricVersions: List<SemanticVersion>,
        val fabricSelectedIndex: Int,
        val forgeVersions: List<SemanticVersion>,
        val forgeSelectedIndex: Int,
        val fabricApiVersions: List<SemanticVersion>,
        val fabricApiSelectedIndex: Int,
        val architecturyApiVersions: List<SemanticVersion>,
        val architecturyApiSelectedIndex: Int
    )
}
