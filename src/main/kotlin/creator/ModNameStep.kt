/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.validation.AFTER_GRAPH_PROPAGATION
import com.intellij.openapi.ui.validation.CHECK_NON_EMPTY
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.*

class ModNameStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val baseData = data.getUserData(NewProjectWizardBaseData.KEY) ?: throw IllegalStateException("Mod name step created without base step")
    val nameProperty = propertyGraph.property(baseData.name)
    var name by nameProperty
    init {
        baseData.nameProperty.afterChange { name = it }
        storeToData()
    }

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("Mod Name:") {
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
        val KEY = Key.create<String>("${ModNameStep::class.java.name}.name")
    }
}