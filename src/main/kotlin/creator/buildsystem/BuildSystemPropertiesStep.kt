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

package com.demonwav.mcdev.creator.buildsystem

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.storeToData
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.ui.validation.CHECK_ARTIFACT_ID
import com.intellij.openapi.ui.validation.CHECK_GROUP_ID
import com.intellij.openapi.ui.validation.CHECK_NON_EMPTY
import com.intellij.openapi.ui.validation.WHEN_GRAPH_PROPAGATION_FINISHED
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

private val nonExampleValidation = validationErrorIf<String>(MCDevBundle("creator.validation.group_id_non_example")) {
    it == "org.example"
}

private val versionValidation = validationErrorIf<String>(MCDevBundle("creator.validation.semantic_version")) {
    SemanticVersion.tryParse(it) == null
}

class BuildSystemPropertiesStep<ParentStep>(private val parent: ParentStep) : AbstractNewProjectWizardStep(parent)
    where ParentStep : NewProjectWizardStep, ParentStep : NewProjectWizardBaseData {

    val groupIdProperty = propertyGraph.property("org.example")
        .bindStorage("${javaClass.name}.groupId")
    val artifactIdProperty = propertyGraph.lazyProperty(::suggestArtifactId)
    val versionProperty = propertyGraph.property("1.0-SNAPSHOT")
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
        builder.collapsibleGroup(MCDevBundle("creator.ui.group.title")) {
            row(MCDevBundle("creator.ui.group.group_id")) {
                textField()
                    .bindText(groupIdProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(propertyGraph))
                    .textValidation(CHECK_NON_EMPTY, CHECK_GROUP_ID, nonExampleValidation)
            }
            row(MCDevBundle("creator.ui.group.artifact_id")) {
                textField()
                    .bindText(artifactIdProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(propertyGraph))
                    .textValidation(CHECK_NON_EMPTY, CHECK_ARTIFACT_ID)
            }
            row(MCDevBundle("creator.ui.group.version")) {
                textField()
                    .bindText(versionProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(propertyGraph))
                    .textValidation(versionValidation)
            }
        }.expanded = true
    }
}
