/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem

import com.demonwav.mcdev.creator.storeToData
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.externalSystem.service.project.wizard.MavenizedNewProjectWizardStep
import com.intellij.openapi.ui.validation.AFTER_GRAPH_PROPAGATION
import com.intellij.openapi.ui.validation.validationTextErrorIf
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.textValidation

private val versionValidation = validationTextErrorIf("Version must be a valid semantic version") {
    SemanticVersion.tryParse(it) == null
}

class BuildSystemPropertiesStep<ParentStep>(parent: ParentStep) :
    MavenizedNewProjectWizardStep<Nothing, ParentStep>(parent)
    where ParentStep : NewProjectWizardStep, ParentStep : NewProjectWizardBaseData {
    override fun createView(data: Nothing) = throw UnsupportedOperationException()
    override fun findAllParents() = emptyList<Nothing>()

    override fun setupAdvancedSettingsUI(builder: Panel) {
        super.setupAdvancedSettingsUI(builder)
        with(builder) {
            row("Version:") {
                textField()
                    .bindText(versionProperty)
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
                    .textValidation(versionValidation)
            }
        }
    }

    init {
        storeToData()
    }
}
