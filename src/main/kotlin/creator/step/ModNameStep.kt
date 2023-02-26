/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.creator.storeToData
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.validation.CHECK_NON_EMPTY
import com.intellij.openapi.ui.validation.WHEN_GRAPH_PROPAGATION_FINISHED
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
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(propertyGraph))
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
    override val label = "Mod Name:"
}

class PluginNameStep(parent: NewProjectWizardStep) : AbstractModNameStep(parent) {
    override val label = "Plugin Name:"
}
