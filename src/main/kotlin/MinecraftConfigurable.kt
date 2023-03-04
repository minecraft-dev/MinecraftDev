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

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.update.ConfigurePluginUpdatesDialog
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent
import org.jetbrains.annotations.Nls

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
        lateinit var showChatColorUnderlinesCheckBox: Cell<JBCheckBox>

        return panel {
            row {
                icon(PlatformAssets.MINECRAFT_ICON)
                text("Minecraft Developpment Settings")
                button("Change plugin update channel") {
                    ConfigurePluginUpdatesDialog().show()
                }.horizontalAlign(HorizontalAlign.RIGHT)
            }
            row {
                text("View Settings")
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
                showChatColorUnderlinesCheckBox = checkBox("Show Chat Color Underlines")
            }
            row {
                text("Chat Color Underline Style")
                comboBox(listOf("Normal", "Bold", "Dotted", "Boxed", "Rounded Boxed", "Waved"))
                    .enabledIf(showChatColorUnderlinesCheckBox.selected)
            }
        }
    }

    override fun createComponent(): JComponent {
        // showChatColorUnderlinesCheckBox.addActionListener { setUnderlineBox() }

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
        // chatColorUnderlinesComboBox.isEnabled = showChatColorUnderlinesCheckBox.isSelected
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
