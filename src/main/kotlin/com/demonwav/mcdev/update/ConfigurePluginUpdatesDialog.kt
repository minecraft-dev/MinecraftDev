/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.updateSettings.impl.UpdateSettings

import java.io.IOException
import java.util.function.Function

import javax.swing.JComponent

class ConfigurePluginUpdatesDialog : DialogWrapper(true) {

    private val form = ConfigurePluginUpdatesForm()
    private var update: PluginUpdateStatus.Update? = null
    private var initialSelectedChannel: Int = 0

    init {
        title = "Configure Minecraft Development Plugin Updates"
        form.updateCheckInProgressIcon.suspend()
        form.updateCheckInProgressIcon.setPaintPassiveIcon(false)

        form.channelBox.addItem("Stable")
        for (channels in Channels.orderedList()) {
            form.channelBox.addItem(channels.title)
        }

        form.checkForUpdatesNowButton.addActionListener {
            saveSettings()
            form.updateCheckInProgressIcon.resume()
            resetUpdateStatus()
            PluginUpdater.runUpdateCheck { pluginUpdateStatus ->
                form.updateCheckInProgressIcon.suspend()

                if (pluginUpdateStatus is PluginUpdateStatus.LatestVersionInstalled) {
                    form.updateStatusLabel.text = "You have the latest version of the plugin (${PluginUtil.pluginVersion}) installed."
                } else if (pluginUpdateStatus is PluginUpdateStatus.Update) {
                    update = pluginUpdateStatus
                    form.installButton.isVisible = true
                    form.updateStatusLabel.text = "A new version (${pluginUpdateStatus.pluginDescriptor.version}) is available"
                } else {
                    // CheckFailed
                    form.updateStatusLabel.text = "Update check failed: " + (pluginUpdateStatus as PluginUpdateStatus.CheckFailed).message
                }

                false
            }
        }

        form.installButton.isVisible = false
        form.installButton.addActionListener {
            update?.let { update ->
                close(DialogWrapper.OK_EXIT_CODE)
                try {
                    PluginUpdater.installPluginUpdate(update)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        form.channelBox.addActionListener { resetUpdateStatus() }

        for (channels in Channels.values()) {
            if (channels.hasChannel()) {
                initialSelectedChannel = channels.index
                break
            }
        }

        form.channelBox.selectedIndex = initialSelectedChannel
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return form.panel
    }

    private fun saveSelectedChannel(channel: Int) {
        val hosts = UpdateSettings.getInstance().storedPluginHosts
        for (channels in Channels.values()) {
            hosts.remove(channels.url)
        }

        val channels = Channels.getChannel(channel) ?: // This really shouldn't happen
            return

        hosts.add(channels.url)
    }

    private fun saveSettings() {
        saveSelectedChannel(form.channelBox.selectedIndex)
    }

    private fun resetUpdateStatus() {
        form.updateStatusLabel.text = " "
        form.installButton.isVisible = false
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
