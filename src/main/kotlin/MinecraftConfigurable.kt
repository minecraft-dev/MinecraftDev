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
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComboBox
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

    private lateinit var showProjectPlatformIconsCheckBox: Cell<JBCheckBox>
    private lateinit var showEventListenerGutterCheckBox: Cell<JBCheckBox>
    private lateinit var showChatColorUnderlinesCheckBox: Cell<JBCheckBox>
    private lateinit var chatColorUnderlinesComboBox: Cell<JComboBox<MinecraftSettings.UnderlineType>>
    private lateinit var showChatGutterIconsCheckBox: Cell<JBCheckBox>

    @Nls
    override fun getDisplayName() = "Minecraft Development"

    override fun getHelpTopic(): String? = null

    fun demoBasics(): DialogPanel {
        val settings = MinecraftSettings.instance

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
                showProjectPlatformIconsCheckBox = checkBox("Show Project Platform Icons")
                    .bindSelected(settings::isShowProjectPlatformIcons)
            }
            row {
                showEventListenerGutterCheckBox = checkBox("Show Event Listener Gutter Icons")
                    .bindSelected(settings::isShowEventListenerGutterIcons)
            }
            row {
                showChatGutterIconsCheckBox = checkBox("Show Chat Color Gutter Icons")
                    .bindSelected(settings::isShowChatColorGutterIcons)
            }
            row {
                showChatColorUnderlinesCheckBox = checkBox("Show Chat Color Underlines")
                    .bindSelected(settings::isShowChatColorUnderlines)
            }
            row {
                text("Chat Color Underline Style")
                chatColorUnderlinesComboBox = comboBox(MinecraftSettings.UnderlineType.values().asList())
                    .enabledIf(showChatColorUnderlinesCheckBox.selected)
            }
        }
    }

    override fun createComponent(): JComponent {
        // showChatColorUnderlinesCheckBox.addActionListener { setUnderlineBox() }

        return demoBasics()
    }

    private fun init() {}

    override fun isModified(): Boolean {
        val settings = MinecraftSettings.instance

        return showProjectPlatformIconsCheckBox.selected.invoke() != settings.isShowProjectPlatformIcons ||
            showEventListenerGutterCheckBox.selected.invoke() != settings.isShowEventListenerGutterIcons ||
            showChatGutterIconsCheckBox.selected.invoke() != settings.isShowChatColorGutterIcons ||
            showChatColorUnderlinesCheckBox.selected.invoke() != settings.isShowChatColorUnderlines /*||
                chatColorUnderlinesComboBox.se !== settings.underlineType*/
    }

    override fun apply() {
        val settings = MinecraftSettings.instance

        settings.isShowProjectPlatformIcons = showProjectPlatformIconsCheckBox.selected.invoke()
        settings.isShowEventListenerGutterIcons = showEventListenerGutterCheckBox.selected.invoke()
        settings.isShowChatColorGutterIcons = showChatGutterIconsCheckBox.selected.invoke()
        settings.isShowChatColorUnderlines = showChatColorUnderlinesCheckBox.selected.invoke()
        // settings.underlineType = chatColorUnderlinesComboBox.selectedItem as MinecraftSettings.UnderlineType
    }

    override fun reset() {
        init()
    }
}
