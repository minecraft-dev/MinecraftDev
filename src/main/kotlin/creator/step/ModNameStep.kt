/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.storeToData
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.validation.AFTER_GRAPH_PROPAGATION
import com.intellij.openapi.ui.validation.CHECK_NON_EMPTY
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

abstract class AbstractModNameStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
        ?: throw IllegalStateException("Mod name step created without base step")
    val nameProperty = propertyGraph.property(baseData.name)
    var name by nameProperty
    init {
        baseData.nameProperty.afterChange { name = it }
        storeToData()
    }

    abstract val label: String

    override fun setupUI(builder: Panel) {
        with(builder) {
            row(label) {
                textField()
                    .bindText(nameProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
                    .textValidation(CHECK_NON_EMPTY)
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, name)
    }

    companion object {
        val KEY = Key.create<String>("${AbstractModNameStep::class.java.name}.name")
    }
}

class ModNameStep(parent: NewProjectWizardStep) : AbstractModNameStep(parent) {
    override val label
        get() = MCDevBundle.message("creator.ui.mod_name.label")
}

class PluginNameStep(parent: NewProjectWizardStep) : AbstractModNameStep(parent) {
    override val label
        get() = MCDevBundle.message("creator.ui.plugin_name.label")
}
