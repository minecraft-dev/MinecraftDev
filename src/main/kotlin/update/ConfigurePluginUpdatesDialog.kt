/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.update

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.updateSettings.PluginUpdateInfo
import com.intellij.openapi.updateSettings.impl.PluginDownloader
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
            button("Check for Updates Now") {
                saveSettings()
                updateCheckInProgressIcon.component.isVisible = true
                resetUpdateStatus()
                PluginUpdater.runUpdateCheck { result ->
                    updateCheckInProgressIcon.component.isVisible = false

                    updateStatusLabel.component.text = result.fold(
                        { info ->
                            when (info) {
                                is PluginUpdateInfo.NoUpdate ->
                                    "You have the latest version of the plugin (${PluginUtil.pluginVersion}) installed."
                                is PluginUpdateInfo.UpdateAvailable -> {
                                    update = info.update
                                    installButton.component.isVisible = true
                                    "A new version (${info.update.pluginVersion}) is available"
                                }
                                is PluginUpdateInfo.CheckFailed ->
                                    "Update check failed: " + info.errors.entries.joinToString("; ") { (host, e) ->
                                        "${host ?: "Marketplace"}: ${e.message}"
                                    }
                            }
                        },
                        { e -> "Update check failed: ${e.message}" },
                    )
                }
            }
            updateCheckInProgressIcon = icon(AnimatedIcon.Default.INSTANCE)
            installButton = button("Install Update") {
                update?.let { update ->
                    close(OK_EXIT_CODE)
                    try {
                        PluginUpdater.installPluginUpdate(update)
                    } catch (e: IOException) {
                        thisLogger().error("Failed to install plugin update", e)
                    }
                }
            }.visible(false)
        }
        row {
            updateStatusLabel = label("")
        }
    }
    private var update: PluginDownloader? = null
    private var initialSelectedChannel: Int = 0

    init {
        title = "Configure Minecraft Development Plugin Updates"
        for (channels in Channels.entries) {
            channelBox.component.addItem(channels.title)
        }

        Channels.entries.forEachIndexed { i, channel ->
            if (channel.hasChannel()) {
                initialSelectedChannel = i + 1
                return@forEachIndexed
            }
        }

        channelBox.component.selectedIndex = initialSelectedChannel

        channelBox.component.addActionListener { resetUpdateStatus() }
        updateCheckInProgressIcon.component.isVisible = false
        init()
    }

    override fun createCenterPanel() = form

    private fun saveSelectedChannel(index: Int) {
        val hosts = UpdateSettings.getInstance().storedPluginHosts
        for (channel in Channels.entries) {
            hosts.remove(channel.url)
        }

        if (index != 0) {
            val channel = Channels.entries[index - 1]
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
