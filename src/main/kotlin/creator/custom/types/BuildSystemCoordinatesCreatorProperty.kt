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

package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.model.BuildSystemCoordinates
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
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

class BuildSystemCoordinatesCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<BuildSystemCoordinates>(descriptor, graph, properties, BuildSystemCoordinates::class.java) {

    private val default = createDefaultValue(descriptor.default)

    override val graphProperty: GraphProperty<BuildSystemCoordinates> = graph.property(default)
    var coords: BuildSystemCoordinates by graphProperty

    private val groupIdProperty = graphProperty.transform({ it.groupId }, { coords.copy(groupId = it) })
    private val artifactIdProperty = graphProperty.transform({ it.artifactId }, { coords.copy(artifactId = it) })
    private val versionProperty = graphProperty.transform({ it.version }, { coords.copy(version = it) })

    override fun createDefaultValue(raw: Any?): BuildSystemCoordinates {
        val str = (raw as? String) ?: return createDefaultValue()
        return deserialize(str)
    }

    private fun createDefaultValue() = BuildSystemCoordinates("org.example", "", "1.0-SNAPSHOT")

    override fun serialize(value: BuildSystemCoordinates): String =
        "${value.groupId}:${value.artifactId}:${value.version}"

    override fun deserialize(string: String): BuildSystemCoordinates {
        val segments = string.split(':')

        val groupId = segments.getOrElse(0) { "" }
        val artifactId = segments.getOrElse(1) { "" }
        val version = segments.getOrElse(2) { "" }
        return BuildSystemCoordinates(groupId, artifactId, version)
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        val projectNameProperty = properties["PROJECT_NAME"]?.graphProperty
        if (projectNameProperty != null) {
            val projectName = projectNameProperty.get()
            if (projectName is String) {
                coords = coords.copy(artifactId = projectName)
            }

            graphProperty.dependsOn(projectNameProperty, false) {
                val newProjectName = projectNameProperty.get()
                if (newProjectName is String) {
                    coords.copy(artifactId = newProjectName)
                } else {
                    coords
                }
            }
        }
    }

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.collapsibleGroup(MCDevBundle("creator.ui.group.title")) {
            this.row(MCDevBundle("creator.ui.group.group_id")) {
                this.textField()
                    .bindText(this@BuildSystemCoordinatesCreatorProperty.groupIdProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(graph))
                    .textValidation(CHECK_NON_EMPTY, CHECK_GROUP_ID, nonExampleValidation)
            }
            this.row(MCDevBundle("creator.ui.group.artifact_id")) {
                this.textField()
                    .bindText(this@BuildSystemCoordinatesCreatorProperty.artifactIdProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(graph))
                    .textValidation(CHECK_NON_EMPTY, CHECK_ARTIFACT_ID)
            }
            this.row(MCDevBundle("creator.ui.group.version")) {
                this.textField()
                    .bindText(this@BuildSystemCoordinatesCreatorProperty.versionProperty)
                    .columns(COLUMNS_MEDIUM)
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(graph))
                    .textValidation(BuiltinValidations.validVersion)
            }
        }.expanded = true
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = BuildSystemCoordinatesCreatorProperty(descriptor, graph, properties)
    }
}
