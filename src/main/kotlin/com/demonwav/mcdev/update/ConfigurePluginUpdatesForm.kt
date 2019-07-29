/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.intellij.util.ui.AsyncProcessIcon
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

class ConfigurePluginUpdatesForm {
    lateinit var channelBox: JComboBox<String>
    lateinit var checkForUpdatesNowButton: JButton
    lateinit var panel: JPanel
    lateinit var updateCheckInProgressIcon: AsyncProcessIcon

    lateinit var updateStatusLabel: JLabel
    lateinit var installButton: JButton

    private fun createUIComponents() {
        updateCheckInProgressIcon = AsyncProcessIcon("Plugin update check in progress")
    }
}
