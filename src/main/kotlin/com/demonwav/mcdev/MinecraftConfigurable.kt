/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev

import com.demonwav.mcdev.update.ConfigurePluginUpdatesDialog
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class MinecraftConfigurable : Configurable {

    private lateinit var panel: JPanel
    private lateinit var showProjectPlatformIconsCheckBox: JCheckBox
    private lateinit var showEventListenerGutterCheckBox: JCheckBox
    private lateinit var showChatColorUnderlinesCheckBox: JCheckBox
    private lateinit var chatColorUnderlinesComboBox: JComboBox<MinecraftSettings.UnderlineType>
    private lateinit var showChatGutterIconsCheckBox: JCheckBox
    private lateinit var enableSideOnlyChecksCheckBox: JCheckBox
    private lateinit var changePluginUpdateChannelButton: JButton

    @Nls
    override fun getDisplayName() = "Minecraft Development"
    override fun getHelpTopic() = null

    override fun createComponent(): JComponent {
        showChatColorUnderlinesCheckBox.addActionListener { setUnderlineBox() }

        return panel
    }

    private fun init() {
        for (type in MinecraftSettings.UnderlineType.values()) {
            chatColorUnderlinesComboBox.addItem(type)
        }

        val settings = MinecraftSettings.instance

        showProjectPlatformIconsCheckBox.isSelected = settings.isShowProjectPlatformIcons
        showEventListenerGutterCheckBox.isSelected = settings.isShowEventListenerGutterIcons
        showChatGutterIconsCheckBox.isSelected = settings.isShowChatColorGutterIcons
        showChatColorUnderlinesCheckBox.isSelected = settings.isShowChatColorUnderlines

        chatColorUnderlinesComboBox.selectedIndex = settings.underlineTypeIndex
        setUnderlineBox()

        enableSideOnlyChecksCheckBox.isSelected = settings.isEnableSideOnlyChecks

        changePluginUpdateChannelButton.addActionListener { ConfigurePluginUpdatesDialog().show() }
    }

    private fun setUnderlineBox() {
        if (showChatColorUnderlinesCheckBox.isSelected) {
            chatColorUnderlinesComboBox.setEnabled(true)
        } else {
            chatColorUnderlinesComboBox.setEnabled(false)
        }
    }

    override fun isModified(): Boolean {
        val settings = MinecraftSettings.instance

        return showProjectPlatformIconsCheckBox.isSelected != settings.isShowProjectPlatformIcons ||
            showEventListenerGutterCheckBox.isSelected != settings.isShowEventListenerGutterIcons ||
            showChatGutterIconsCheckBox.isSelected != settings.isShowChatColorGutterIcons ||
            showChatColorUnderlinesCheckBox.isSelected != settings.isShowChatColorUnderlines ||
            chatColorUnderlinesComboBox.selectedItem !== settings.underlineType ||
            enableSideOnlyChecksCheckBox.isSelected != settings.isEnableSideOnlyChecks
    }

    override fun apply() {
        val settings = MinecraftSettings.instance

        settings.isShowProjectPlatformIcons = showProjectPlatformIconsCheckBox.isSelected
        settings.isShowEventListenerGutterIcons = showEventListenerGutterCheckBox.isSelected
        settings.isShowChatColorGutterIcons = showChatGutterIconsCheckBox.isSelected
        settings.isShowChatColorUnderlines = showChatColorUnderlinesCheckBox.isSelected
        settings.underlineType = chatColorUnderlinesComboBox.selectedItem as MinecraftSettings.UnderlineType
        settings.isEnableSideOnlyChecks = enableSideOnlyChecksCheckBox.isSelected
    }

    override fun reset() {
        init()
    }
}
