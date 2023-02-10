/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem

import com.demonwav.mcdev.creator.storeToData
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.ui.validation.AFTER_GRAPH_PROPAGATION
import com.intellij.openapi.ui.validation.CHECK_ARTIFACT_ID_FORMAT
import com.intellij.openapi.ui.validation.CHECK_GROUP_ID_FORMAT
import com.intellij.openapi.ui.validation.CHECK_NON_EMPTY
import com.intellij.openapi.ui.validation.validationTextErrorIf
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

private val nonExampleValidation = validationTextErrorIf("Group ID must be changed from \"org.example\"") {
    it == "org.example"
}

private val versionValidation = validationTextErrorIf("Version must be a valid semantic version") {
    SemanticVersion.tryParse(it) == null
}

class BuildSystemPropertiesStep<ParentStep>(private val parent: ParentStep) : AbstractNewProjectWizardStep(parent)
    where ParentStep : NewProjectWizardStep, ParentStep : NewProjectWizardBaseData {

    val groupIdProperty = propertyGraph.property("org.example")
        .bindStorage("${javaClass.name}.groupId")
    val artifactIdProperty = propertyGraph.lazyProperty(::suggestArtifactId)
    private val versionProperty = propertyGraph.property("1.0-SNAPSHOT")
        .bindStorage("${javaClass.name}.version")

    var groupId by groupIdProperty
    var artifactId by artifactIdProperty
    var version by versionProperty

    init {
        artifactIdProperty.dependsOn(parent.nameProperty, ::suggestArtifactId)
        storeToData()
    }

    private fun suggestArtifactId() = parent.name

    override fun setupUI(builder: Panel) {
        builder.collapsibleGroup("Build System Properties") {
            row("Group ID:") {
                textField()
                    .bindText(groupIdProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
                    .textValidation(CHECK_NON_EMPTY, CHECK_GROUP_ID_FORMAT, nonExampleValidation)
            }
            row("Artifact ID:") {
                textField()
                    .bindText(artifactIdProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
                    .textValidation(CHECK_NON_EMPTY, CHECK_ARTIFACT_ID_FORMAT)
            }
            row("Version:") {
                textField()
                    .bindText(versionProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
                    .textValidation(versionValidation)
            }
        }.expanded = true
    }
}
