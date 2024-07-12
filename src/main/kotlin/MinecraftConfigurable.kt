/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.custom.templateRepoTable
import com.demonwav.mcdev.update.ConfigurePluginUpdatesDialog
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.Label
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.IconUtil
import javax.swing.JComponent
import org.jetbrains.annotations.Nls

class MinecraftConfigurable : Configurable {

    private lateinit var panel: DialogPanel

    @Nls
    override fun getDisplayName() = MCDevBundle("minecraft.settings.display_name")

    override fun createComponent(): JComponent = panel {
        row(
            Label(MCDevBundle("minecraft.settings.title"), bold = true).apply {
                font = font.deriveFont(font.size * 1.5f)
                icon = IconUtil.scale(PlatformAssets.MINECRAFT_ICON_2X, null, 1.5f)
            }
        ) {
            button(MCDevBundle("minecraft.settings.change_update_channel")) {
                ConfigurePluginUpdatesDialog().show()
            }.align(AlignX.RIGHT)
        }.bottomGap(BottomGap.MEDIUM)

        val settings = MinecraftSettings.instance

        row {
            checkBox(MCDevBundle("minecraft.settings.show_project_platform_icons"))
                .bindSelected(settings::isShowProjectPlatformIcons)
        }
        row {
            checkBox(MCDevBundle("minecraft.settings.show_event_listener_gutter_icons"))
                .bindSelected(settings::isShowEventListenerGutterIcons)
        }
        row {
            checkBox(MCDevBundle("minecraft.settings.show_chat_color_gutter_icons"))
                .bindSelected(settings::isShowChatColorGutterIcons)
        }
        row {
            checkBox(MCDevBundle("minecraft.settings.show_chat_color_underlines"))
                .bindSelected(settings::isShowChatColorUnderlines)
        }.bottomGap(BottomGap.SMALL)

        group(indent = false) {
            row(MCDevBundle("minecraft.settings.chat_color_underline_style")) {
                comboBox(EnumComboBoxModel(MinecraftSettings.UnderlineType::class.java))
                    .bindItem(settings::underlineType) { settings.underlineType = it!! }
                    .align(AlignX.LEFT)
            }
        }

        group(MCDevBundle("minecraft.settings.creator")) {
            row(MCDevBundle("minecraft.settings.creator.repos")) {}

            row {
                templateRepoTable(
                    MutableProperty(
                        { settings.creatorTemplateRepos.toMutableList() },
                        { settings.creatorTemplateRepos = it }
                    )
                )
            }.resizableRow()
        }

        onApply {
            for (project in ProjectManager.getInstance().openProjects) {
                ProjectView.getInstance(project).refresh()
            }
        }
    }.also { panel = it }

    override fun isModified(): Boolean = panel.isModified()

    override fun apply() = panel.apply()

    override fun reset() = panel.reset()
}
