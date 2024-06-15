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
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.demonwav.mcdev.update.ConfigurePluginUpdatesDialog
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.impl.cache.impl.id.IdDataConsumer
import com.intellij.ui.ComboBoxTableCellRenderer
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.Label
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.TableView
import com.intellij.util.IconUtil
import com.intellij.util.ListWithSelection
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.ComboBoxTableCellEditor
import javax.swing.JComponent
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
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

        group(MCDevBundle("minecraft.settings.mixin")) {
            row {
                checkBox(MCDevBundle("minecraft.settings.mixin.shadow_annotation_same_line"))
                    .bindSelected(settings::isShadowAnnotationsSameLine)
            }
        }

        val nameColumn = object :
            ColumnInfo<MinecraftSettings.TemplateRepo, String>(
                MCDevBundle("minecraft.settings.creator.repos.column.name")
            ) {

            override fun valueOf(item: MinecraftSettings.TemplateRepo?): String? {
                return item?.name
            }

            override fun setValue(item: MinecraftSettings.TemplateRepo?, value: String?) {
                item?.name = value ?: MCDevBundle("minecraft.settings.creator.repo.default_name")
            }

            override fun isCellEditable(item: MinecraftSettings.TemplateRepo?): Boolean = true
        }

        val providerColumn = object : ColumnInfo<MinecraftSettings.TemplateRepo, Any>(
            MCDevBundle("minecraft.settings.creator.repos.column.provider")
        ) {

            override fun valueOf(item: MinecraftSettings.TemplateRepo?): ListWithSelection<String>? {
                val providers = TemplateProvider.getAllKeys()
                val list = ListWithSelection<String>(providers)
                list.select(item?.provider?.takeUnless { it.isBlank() })

                return list
            }

            override fun setValue(item: MinecraftSettings.TemplateRepo?, value: Any?) {
                item?.provider = value as? String ?: "local"
            }

            override fun isCellEditable(item: MinecraftSettings.TemplateRepo?): Boolean = true

            override fun getRenderer(item: MinecraftSettings.TemplateRepo?): TableCellRenderer? {
                return ComboBoxTableCellRenderer.INSTANCE
            }

            override fun getEditor(item: MinecraftSettings.TemplateRepo?): TableCellEditor? {
                return ComboBoxTableCellEditor.INSTANCE
            }
        }

        val model = object : ListTableModel<MinecraftSettings.TemplateRepo>(nameColumn, providerColumn) {
            override fun addRow() {
                val defaultName = MCDevBundle("minecraft.settings.creator.repo.default_name")
                addRow(MinecraftSettings.TemplateRepo(defaultName, "local", ""))
            }
        }
        group(MCDevBundle("minecraft.settings.creator")) {
            row(MCDevBundle("minecraft.settings.creator.repos")) {}

            row {
                val table = TableView<MinecraftSettings.TemplateRepo>()
                table.setShowGrid(true)
                table.model = model
                table.tableHeader.reorderingAllowed = false

                val decoratedTable = ToolbarDecorator.createDecorator(table)
                    .setEditActionUpdater {
                        val selectedRepo = table.selection.firstOrNull()
                            ?: return@setEditActionUpdater false
                        val provider = TemplateProvider.get(selectedRepo.provider)
                            ?: return@setEditActionUpdater false
                        return@setEditActionUpdater provider.hasConfig
                    }
                    .setEditAction {
                        val selectedRepo = table.selection.firstOrNull()
                            ?: return@setEditAction
                        val provider = TemplateProvider.get(selectedRepo.provider)
                            ?: return@setEditAction
                        val dataConsumer = { data: String -> selectedRepo.data = data }
                        val configPanel = provider.setupConfigUi(selectedRepo.data, dataConsumer)
                            ?: return@setEditAction

                        val dialog = object : DialogWrapper(null) {
                            init {
                                init()
                            }

                            override fun createCenterPanel(): JComponent = configPanel
                        }
                        dialog.title = MCDevBundle("minecraft.settings.creator.repo_config.title", selectedRepo.name)
                        dialog.show()
                    }
                    .createPanel()
                cell(decoratedTable)
                    .align(Align.FILL)
                    .bind(
                        { _ -> model.items },
                        { _, repos -> model.items = repos; },
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
