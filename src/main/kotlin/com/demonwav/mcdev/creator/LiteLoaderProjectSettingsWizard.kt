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

import com.demonwav.mcdev.exception.EmptyInputSetupException
import com.demonwav.mcdev.exception.SetupException
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.liteloader.LiteLoaderProjectConfiguration
import com.demonwav.mcdev.platform.liteloader.version.LiteLoaderVersion
import com.demonwav.mcdev.platform.mcp.version.McpVersion
import com.demonwav.mcdev.platform.mcp.version.McpVersionEntry
import com.demonwav.mcdev.util.firstOfType
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.apache.commons.lang.WordUtils

class LiteLoaderProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var panel: JPanel
    private lateinit var mcpWarning: JLabel
    private lateinit var modNameField: JTextField
    private lateinit var modVersionField: JTextField
    private lateinit var mainClassField: JTextField
    private lateinit var minecraftVersionBox: JComboBox<String>
    private lateinit var mcpVersionBox: JComboBox<McpVersionEntry>
    private lateinit var loadingBar: JProgressBar

    private var config: LiteLoaderProjectConfiguration? = null

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

                    mainClassModified = true
                    (e as AbstractDocument.DefaultDocumentEvent).undo()
                    mainClassField.document.addDocumentListener(this)
                }
            }
        }
    }

    init {
        mcpWarning.isVisible = false

        minecraftVersionBox.addActionListener {
            val mcpVersion = versions?.mcpVersion ?: return@addActionListener
            CoroutineScope(Dispatchers.Swing).launch {
                val version = minecraftVersionBox.selectedItem as String
                val mcpVersions = withContext(Dispatchers.Default) { mcpVersion.getMcpVersionList(version) }

                mcpVersionBox.removeActionListener(mcpBoxActionListener)
                mcpVersionBox.removeAllItems()
                mcpVersions.forEach { mcpVersionBox.addItem(it) }
                mcpVersionBox.addActionListener(mcpBoxActionListener)
                mcpBoxActionListener.actionPerformed(null)
            }
        }

        modNameField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (mainClassModified) {
                    return
                }

                val word = modNameField.text.split(Regex("\\s+")).joinToString { WordUtils.capitalize(it) }

                val mainClassWords = mainClassField.text.split('.').toTypedArray()
                mainClassWords[mainClassWords.size - 1] = LITEMOD + word

                mainClassField.document.removeDocumentListener(listener)
                mainClassField.text = mainClassWords.joinToString(".")
                mainClassField.document.addDocumentListener(listener)
            }
        })

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

            if (modVersionField.text.isBlank()) {
                throw EmptyInputSetupException(modVersionField)
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
        return creator.configs.any { it is LiteLoaderProjectConfiguration }
    }

    override fun updateStep() {
        config = creator.configs.firstOfType()
        if (config == null) {
            return
        }

        val buildSystem = creator.buildSystem ?: return

        modNameField.text = WordUtils.capitalizeFully(buildSystem.artifactId.replace('-', ' '))
        modVersionField.text = buildSystem.version

        val conf = config ?: return

        if (creator.configs.indexOf(conf) != 0) {
            modNameField.isEditable = false
            modVersionField.isEditable = false
        }

        mainClassField.document.removeDocumentListener(listener)
        mainClassField.text = buildSystem.groupId.replace("-", "") + "." +
            buildSystem.artifactId.replace("-", "") + "." + LITEMOD +
            WordUtils.capitalizeFully(buildSystem.artifactId.replace('-', ' ')).replace(" ", "")
        mainClassField.document.addDocumentListener(listener)

        if (versions != null || currentJob?.isActive == true) {
            return
        }
        currentJob = updateVersions()
    }

    override fun onStepLeaving() {
        currentJob?.let { job ->
            // we're in a cancel state
            job.cancel()
            return
        }

        val conf = config ?: return
        conf.base = ProjectConfiguration.BaseConfigs(
            modNameField.text,
            modVersionField.text,
            mainClassField.text
        )

        conf.mcVersion = minecraftVersionBox.selectedItem as String
        conf.mcpVersion = (mcpVersionBox.selectedItem as McpVersionEntry).versionPair
    }

    override fun updateDataModel() {}

    private fun updateVersions() = CoroutineScope(Dispatchers.Swing).launch {
        loadingBar.isIndeterminate = true
        loadingBar.isVisible = true

        val version = minecraftVersionBox.selectedItem as? String

        val mcpVersionJob = async(Dispatchers.IO) { McpVersion.downloadData() }
        val liteloaderVersionJob = async(Dispatchers.IO) { LiteLoaderVersion.downloadData() }

        val mcpVersion = mcpVersionJob.await() ?: return@launch
        val liteloaderVersion = liteloaderVersionJob.await() ?: return@launch

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
