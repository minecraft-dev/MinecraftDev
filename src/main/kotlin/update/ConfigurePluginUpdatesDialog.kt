/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import java.io.IOException
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel

class ConfigurePluginUpdatesDialog : DialogWrapper(true) {

    private lateinit var channelBox: Cell<JComboBox<String>>
    private lateinit var installButton: Cell<JButton>
    private lateinit var updateStatusLabel: Cell<JLabel>
    private lateinit var updateCheckInProgressIcon: Cell<JLabel>

    private val form = panel {
        row {
            label("Update channel:")
            channelBox = comboBox(listOf("Stable"))
        }
        row {
            button("Check for updates now") {
                saveSettings()
                updateCheckInProgressIcon.component.isVisible = true
                resetUpdateStatus()
                PluginUpdater.runUpdateCheck { pluginUpdateStatus ->
                    updateCheckInProgressIcon.component.isVisible = false

                    updateStatusLabel.component.text = when (pluginUpdateStatus) {
                        is PluginUpdateStatus.LatestVersionInstalled ->
                            "You have the latest version of the plugin (${PluginUtil.pluginVersion}) installed."
                        is PluginUpdateStatus.Update -> {
                            update = pluginUpdateStatus
                            installButton.component.isVisible = true
                            "A new version (${pluginUpdateStatus.pluginDescriptor.version}) is available"
                        }
                        else -> // CheckFailed
                            "Update check failed: " + (pluginUpdateStatus as PluginUpdateStatus.CheckFailed).message
                    }

                    false
                }
            }
            updateCheckInProgressIcon = icon(AnimatedIcon.Default.INSTANCE)
            installButton = button("Install Update") {
                update?.let { update ->
                    close(OK_EXIT_CODE)
                    try {
                        PluginUpdater.installPluginUpdate(update)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }.visible(false)
        }
        row {
            updateStatusLabel = label("")
        }
    }
    private var update: PluginUpdateStatus.Update? = null
    private var initialSelectedChannel: Int = 0

    init {
        title = "Configure Minecraft Development Plugin Updates"
        for (channels in Channels.values()) {
            channelBox.component.addItem(channels.title)
        }

        channelBox.component.selectedIndex = initialSelectedChannel

        Channels.values().forEachIndexed { i, channel ->
            if (channel.hasChannel()) {
                initialSelectedChannel = i + 1
                return@forEachIndexed
            }
        }

        channelBox.component.addActionListener { resetUpdateStatus() }
        updateCheckInProgressIcon.component.isVisible = false
        init()
    }

    override fun createCenterPanel() = form

    private fun saveSelectedChannel(index: Int) {
        val hosts = UpdateSettings.getInstance().storedPluginHosts
        for (channel in Channels.values()) {
            hosts.remove(channel.url)
        }

        if (index != 0) {
            val channel = Channels.values()[index - 1]
            hosts.add(channel.url)
        }
    }

    private fun saveSettings() {
        saveSelectedChannel(channelBox.component.selectedIndex)
    }

    private fun resetUpdateStatus() {
        updateStatusLabel.component.text = " "
        installButton.component.isVisible = false
    }

    override fun doOKAction() {
        saveSettings()
        super.doOKAction()
    }

    override fun doCancelAction() {
        saveSelectedChannel(initialSelectedChannel)
        super.doCancelAction()
    }
}
