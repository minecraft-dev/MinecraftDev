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

package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ComboBoxTableCellRenderer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.table.TableView
import com.intellij.util.ListWithSelection
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.ComboBoxTableCellEditor
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

private object NameColumn : ColumnInfo<MinecraftSettings.TemplateRepo, String>(
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

private object ProviderColumn : ColumnInfo<MinecraftSettings.TemplateRepo, Any>(
    MCDevBundle("minecraft.settings.creator.repos.column.provider")
) {
    override fun valueOf(item: MinecraftSettings.TemplateRepo?): ListWithSelection<String>? {
        val providers = TemplateProvider.getAllKeys()
        val list = ListWithSelection<String>(providers)
        list.select(item?.provider?.takeIf(providers::contains))

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

fun Row.templateRepoTable(
    prop: MutableProperty<List<MinecraftSettings.TemplateRepo>>
): Cell<JPanel> {
    val model = object : ListTableModel<MinecraftSettings.TemplateRepo>(NameColumn, ProviderColumn) {
        override fun addRow() {
            val defaultName = MCDevBundle("minecraft.settings.creator.repo.default_name")
            addRow(MinecraftSettings.TemplateRepo(defaultName, "local", ""))
        }
    }

    val table = TableView<MinecraftSettings.TemplateRepo>(model)
    table.setShowGrid(true)
    table.tableHeader.reorderingAllowed = false

    val decoratedTable = ToolbarDecorator.createDecorator(table)
        .setPreferredSize(Dimension(JBUI.scale(300), JBUI.scale(200)))
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

            val dialog = object : DialogWrapper(table, true) {
                init {
                    init()
                }

                override fun createCenterPanel(): JComponent = configPanel
            }
            dialog.title = MCDevBundle("minecraft.settings.creator.repo_config.title", selectedRepo.name)
            dialog.show()
        }
        .createPanel()
    return cell(decoratedTable)
        .bind(
            { _ -> model.items },
            { _, repos -> model.items = repos; },
            prop
        )
}
