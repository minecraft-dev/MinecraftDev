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

import com.demonwav.mcdev.creator.storeToData
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.validation.AFTER_GRAPH_PROPAGATION
import com.intellij.openapi.ui.validation.CHECK_NON_EMPTY
import com.intellij.openapi.ui.validation.and
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

private val validModIdRegex = "[a-z][a-z0-9-_]{1,63}".toRegex()
private val invalidModIdRegex = "[^a-z0-9-_]+".toRegex()

private val validForgeModIdRegex = "[a-z][a-z0-9_]{1,63}".toRegex()
private val invalidForgeModIdRegex = "[^a-z0-9_]+".toRegex()

abstract class AbstractModIdStep(
    parent: NewProjectWizardStep,
    private val validRegex: Regex = validModIdRegex,
    private val invalidRegex: Regex = invalidModIdRegex
) : AbstractNewProjectWizardStep(parent) {
    private val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
        ?: throw IllegalStateException("Mod id step created without base step")
    val idProperty = propertyGraph.lazyProperty(::suggestId)
    var id by idProperty

    private val idValidation = validationErrorIf<String>("Id must match $validRegex") { !it.matches(validRegex) }

    init {
        idProperty.dependsOn(baseData.nameProperty, ::suggestId)
        storeToData()
    }

    fun suggestId(): String {
        val sanitized = baseData.name.lowercase().replace(invalidRegex, "_")
        if (sanitized.length > 64) {
            return sanitized.substring(0, 64)
        }
        return sanitized
    }

    abstract val label: String

    override fun setupUI(builder: Panel) {
        with(builder) {
            row(label) {
                textField()
                    .bindText(idProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
                    .textValidation(CHECK_NON_EMPTY and idValidation)
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, id)
    }

    companion object {
        val KEY = Key.create<String>("${AbstractModIdStep::class.java.name}.id")
    }
}

class ModIdStep(parent: NewProjectWizardStep) : AbstractModIdStep(parent) {
    override val label = "Mod Id:"
}

class ForgeStyleModIdStep(parent: NewProjectWizardStep) :
    AbstractModIdStep(parent, validForgeModIdRegex, invalidForgeModIdRegex) {
    override val label = "Mod Id:"
}

class PluginIdStep(parent: NewProjectWizardStep) : AbstractModIdStep(parent) {
    override val label = "Plugin Id:"
}
