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
import com.demonwav.mcdev.util.BeforeOrAfter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import org.jetbrains.annotations.Nls

class MinecraftProjectConfigurable(private val project: Project) : Configurable {
    private lateinit var panel: DialogPanel

    @Nls
    override fun getDisplayName() = MCDevBundle("minecraft.settings.project.display_name")

    override fun createComponent(): JComponent = panel {
        val settings = MinecraftProjectSettings.getInstance(project)

        group(MCDevBundle("minecraft.settings.mixin")) {
            row {
                checkBox(MCDevBundle("minecraft.settings.mixin.shadow_annotation_same_line"))
                    .bindSelected(settings::isShadowAnnotationsSameLine)
            }
            row {
                label(MCDevBundle("minecraft.settings.mixin.definition_pos_relative_to_expression"))
                comboBox(EnumComboBoxModel(BeforeOrAfter::class.java))
                    .bindItem(settings::definitionPosRelativeToExpression) {
                        settings.definitionPosRelativeToExpression = it ?: BeforeOrAfter.BEFORE
                    }
            }
        }
    }.also { panel = it }

    override fun isModified(): Boolean = panel.isModified()

    override fun apply() = panel.apply()

    override fun reset() = panel.reset()
}
