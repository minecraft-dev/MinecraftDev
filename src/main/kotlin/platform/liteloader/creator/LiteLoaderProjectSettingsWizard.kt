/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader.creator

import com.demonwav.mcdev.creator.MinecraftModuleWizardStep
import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.creator.ValidatedField
import com.demonwav.mcdev.creator.ValidatedFieldType.CLASS_NAME
import com.demonwav.mcdev.creator.ValidatedFieldType.NON_BLANK
import com.demonwav.mcdev.creator.exception.EmptyInputSetupException
import com.demonwav.mcdev.creator.exception.SetupException
import com.demonwav.mcdev.platform.liteloader.version.LiteLoaderVersion
import com.demonwav.mcdev.platform.mcp.version.McpVersion
import com.demonwav.mcdev.platform.mcp.version.McpVersionEntry
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.invokeLater
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.awt.RelativePoint
import java.awt.event.ActionListener
import java.util.regex.Pattern
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.text.AbstractDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.apache.commons.lang.WordUtils

class LiteLoaderProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var panel: JPanel
    private lateinit var mcpWarning: JLabel

    @ValidatedField(NON_BLANK)
    private lateinit var modNameField: JTextField

    @ValidatedField(NON_BLANK, CLASS_NAME)
    private lateinit var mainClassField: JTextField
    private lateinit var minecraftVersionBox: JComboBox<SemanticVersion>
    private lateinit var mcpVersionBox: JComboBox<McpVersionEntry>
    private lateinit var loadingBar: JProgressBar

    private var config: LiteLoaderProjectConfig? = null

    private data class LiteLoaderVersions(
        val mcpVersion: McpVersion,
        val liteloaderVersion: LiteLoaderVersion
    )

    private var versions: LiteLoaderVersions? = null
    private var mainClassModified = false

    private var currentJob: Job? = null

    private val mcpBoxActionListener = ActionListener {
        mcpWarning.isVisible = (mcpVersionBox.selectedItem as McpVersionEntry).isRed
    }

    private val listener = object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
            // Make sure they don't try to add spaces or whatever
            if (javaClassPattern.matcher(mainClassField.text).find()) {
                invokeLater {
                    mainClassField.document.removeDocumentListener(this)
                    (e as AbstractDocument.DefaultDocumentEvent).undo()
                    mainClassField.document.addDocumentListener(this)
                }
                return
            }

            // We just need to make sure they aren't messing up the LiteMod text
            val words = mainClassField.text.split(".").dropLastWhile(String::isEmpty).toTypedArray()
            if (!words.last().startsWith(LITEMOD)) {
                invokeLater {
                    mainClassField.document.removeDocumentListener(this)
                    (e as AbstractDocument.DefaultDocumentEvent).undo()
                    mainClassField.document.addDocumentListener(this)
                }
            } else {
                mainClassModified = true
            }
        }
    }

    init {
        mcpWarning.isVisible = false

        minecraftVersionBox.addActionListener {
            val mcpVersion = versions?.mcpVersion ?: return@addActionListener
            CoroutineScope(Dispatchers.Swing).launch {
                val version = minecraftVersionBox.selectedItem as SemanticVersion
                val mcpVersions = withContext(Dispatchers.Default) { mcpVersion.getMcpVersionList(version) }

                mcpVersionBox.removeActionListener(mcpBoxActionListener)
                mcpVersionBox.removeAllItems()
                mcpVersions.forEach { mcpVersionBox.addItem(it) }
                mcpVersionBox.addActionListener(mcpBoxActionListener)
                mcpBoxActionListener.actionPerformed(null)
            }
        }

        modNameField.document.addDocumentListener(
            object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    if (mainClassModified) {
                        return
                    }

                    val word = modNameField.text.split(Regex("\\s+")).joinToString("") { WordUtils.capitalize(it) }

                    val mainClassWords = mainClassField.text.split('.').toTypedArray()
                    mainClassWords[mainClassWords.size - 1] = LITEMOD + word

                    mainClassField.document.removeDocumentListener(listener)
                    mainClassField.text = mainClassWords.joinToString(".")
                    mainClassField.document.addDocumentListener(listener)
                }
            }
        )

        mainClassField.document.addDocumentListener(listener)
    }

    override fun getComponent(): JComponent {
        return panel
    }

    override fun validate(): Boolean {
        try {
            if (modNameField.text.isBlank()) {
                throw EmptyInputSetupException(modNameField)
            }

            if (mainClassField.text.isBlank()) {
                throw EmptyInputSetupException(mainClassField)
            }
        } catch (e: SetupException) {
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(e.error, MessageType.ERROR, null)
                .setFadeoutTime(4000)
                .createBalloon()
                .show(RelativePoint.getSouthWestOf(e.j), Balloon.Position.below)
            return false
        }

        return !loadingBar.isVisible
    }

    override fun isStepVisible(): Boolean {
        return creator.config is LiteLoaderProjectConfig
    }

    override fun updateStep() {
        config = creator.config as? LiteLoaderProjectConfig
        if (config == null) {
            return
        }

        val buildSystem = creator.buildSystem ?: return

        modNameField.text = WordUtils.capitalizeFully(buildSystem.artifactId.replace('-', ' '))

        mainClassField.document.removeDocumentListener(listener)
        mainClassField.text = generateClassName(buildSystem, modNameField.text) { name -> LITEMOD + name }
        mainClassField.document.addDocumentListener(listener)

        if (versions != null || currentJob?.isActive == true) {
            return
        }
        currentJob = updateVersions()
    }

    override fun onStepLeaving() {
        currentJob?.cancel()
    }

    override fun updateDataModel() {
        val conf = this.config ?: return

        conf.pluginName = this.modNameField.text
        conf.mainClass = this.mainClassField.text

        conf.mcVersion = this.minecraftVersionBox.selectedItem as SemanticVersion
        conf.mcpVersion = (this.mcpVersionBox.selectedItem as McpVersionEntry).versionPair
    }

    private fun updateVersions() = CoroutineScope(Dispatchers.Swing).launch {
        loadingBar.isIndeterminate = true
        loadingBar.isVisible = true

        val version = minecraftVersionBox.selectedItem as? SemanticVersion

        val mcpVersionJob = asyncIO { McpVersion.downloadData() }
        val liteloaderVersionJob = asyncIO { LiteLoaderVersion.downloadData() }

        val (mcpVersionObj, liteloaderVersionObj) = listOf(mcpVersionJob, liteloaderVersionJob).awaitAll()
        val mcpVersion = mcpVersionObj as McpVersion? ?: return@launch
        val liteloaderVersion = liteloaderVersionObj as LiteLoaderVersion? ?: return@launch

        val data = withContext(Dispatchers.IO) {
            val listVersion = version ?: liteloaderVersion.sortedMcVersions.first()
            return@withContext mcpVersion.getMcpVersionList(listVersion)
        }

        if (liteloaderVersion.sortedMcVersions.isEmpty()) {
            return@launch
        }

        minecraftVersionBox.removeAllItems()

        liteloaderVersion.sortedMcVersions.forEach { minecraftVersionBox.addItem(it) }
        // Always select most recent
        minecraftVersionBox.selectedIndex = 0

        mcpVersionBox.removeActionListener(mcpBoxActionListener)
        mcpVersionBox.removeAllItems()
        data.forEach { mcpVersionBox.addItem(it) }
        mcpVersionBox.addActionListener(mcpBoxActionListener)
        mcpBoxActionListener.actionPerformed(null)

        versions = LiteLoaderVersions(mcpVersion, liteloaderVersion)

        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false

        currentJob = null
    }

    companion object {
        private const val LITEMOD = "LiteMod"
        private val javaClassPattern = Pattern.compile("\\s+|-|\\$")
    }
}
