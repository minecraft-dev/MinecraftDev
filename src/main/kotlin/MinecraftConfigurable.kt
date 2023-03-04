/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev

import com.demonwav.mcdev.update.ConfigurePluginUpdatesDialog
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.textField

class MinecraftConfigurable : Configurable {

    /*private lateinit var panel: JPanel
    private lateinit var showProjectPlatformIconsCheckBox: JCheckBox
    private lateinit var showEventListenerGutterCheckBox: JCheckBox
    private lateinit var showChatColorUnderlinesCheckBox: JCheckBox
    private lateinit var chatColorUnderlinesComboBox: JComboBox<MinecraftSettings.UnderlineType>
    private lateinit var showChatGutterIconsCheckBox: JCheckBox
    private lateinit var changePluginUpdateChannelButton: JButton*/

    @Nls
    override fun getDisplayName() = "Minecraft Development"

    override fun getHelpTopic(): String? = null

    fun demoBasics(): DialogPanel {
        return panel {
            row {
                text("Minecraft Developpment Settings")
                button("Change plugin update channel") {
                    ConfigurePluginUpdatesDialog().show()
                }
            }
            row {
                checkBox("Show Project Platform Icons")
            }
            row {
                checkBox("Show Event Listener Gutter Icons")
            }
            row {
                checkBox("Show Chat Color Gutter Icons")
            }
            row {
                checkBox("Show Chat Color Underlines")
            }
            row {
                text("Chat Color Underline Style")
            }
        }
    }

    override fun createComponent(): JComponent {
        //showChatColorUnderlinesCheckBox.addActionListener { setUnderlineBox() }

        return demoBasics()
    }

    private fun init() {
        /*for (type in MinecraftSettings.UnderlineType.values()) {
            chatColorUnderlinesComboBox.addItem(type)
        }

        val settings = MinecraftSettings.instance

        showProjectPlatformIconsCheckBox.isSelected = settings.isShowProjectPlatformIcons
        showEventListenerGutterCheckBox.isSelected = settings.isShowEventListenerGutterIcons
        showChatGutterIconsCheckBox.isSelected = settings.isShowChatColorGutterIcons
        showChatColorUnderlinesCheckBox.isSelected = settings.isShowChatColorUnderlines

        chatColorUnderlinesComboBox.selectedIndex = settings.underlineTypeIndex
        setUnderlineBox()

        changePluginUpdateChannelButton.addActionListener { ConfigurePluginUpdatesDialog().show() }*/
    }

    private fun setUnderlineBox() {
        //chatColorUnderlinesComboBox.isEnabled = showChatColorUnderlinesCheckBox.isSelected
    }

    override fun isModified(): Boolean {
        val settings = MinecraftSettings.instance

        /*return showProjectPlatformIconsCheckBox.isSelected != settings.isShowProjectPlatformIcons ||
            showEventListenerGutterCheckBox.isSelected != settings.isShowEventListenerGutterIcons ||
            showChatGutterIconsCheckBox.isSelected != settings.isShowChatColorGutterIcons ||
            showChatColorUnderlinesCheckBox.isSelected != settings.isShowChatColorUnderlines ||
            chatColorUnderlinesComboBox.selectedItem !== settings.underlineType*/

        return false
    }

    override fun apply() {
        /*val settings = MinecraftSettings.instance

        settings.isShowProjectPlatformIcons = showProjectPlatformIconsCheckBox.isSelected
        settings.isShowEventListenerGutterIcons = showEventListenerGutterCheckBox.isSelected
        settings.isShowChatColorGutterIcons = showChatGutterIconsCheckBox.isSelected
        settings.isShowChatColorUnderlines = showChatColorUnderlinesCheckBox.isSelected
        settings.underlineType = chatColorUnderlinesComboBox.selectedItem as MinecraftSettings.UnderlineType*/
    }

    override fun reset() {
        init()
    }
}
