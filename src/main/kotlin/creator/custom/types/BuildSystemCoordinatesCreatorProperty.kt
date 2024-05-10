package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.model.BuildSystemCoordinates
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

class BuildSystemCoordinatesCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<BuildSystemCoordinates>(descriptor, graph, properties) {

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

    private fun createDefaultValue() = BuildSystemCoordinates("", "", "")

    override fun serialize(value: BuildSystemCoordinates): String =
        "${value.groupId}:${value.artifactId}:${value.version}"

    override fun deserialize(string: String): BuildSystemCoordinates {
        val segments = string.split(':')

        val groupId = segments.getOrElse(0) { "" }
        val artifactId = segments.getOrElse(1) { "" }
        val version = segments.getOrElse(2) { "" }
        return BuildSystemCoordinates(groupId, artifactId, version)
    }

    override fun setupProperty() {
        super.setupProperty()

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
//                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(graph))
//                    .textValidation(CHECK_NON_EMPTY, CHECK_GROUP_ID, nonExampleValidation)
            }
            this.row(MCDevBundle("creator.ui.group.artifact_id")) {
                this.textField()
                    .bindText(this@BuildSystemCoordinatesCreatorProperty.artifactIdProperty)
                    .columns(COLUMNS_MEDIUM)
//                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(graph))
//                    .textValidation(CHECK_NON_EMPTY, CHECK_ARTIFACT_ID)
            }
            this.row(MCDevBundle("creator.ui.group.version")) {
                this.textField()
                    .bindText(this@BuildSystemCoordinatesCreatorProperty.versionProperty)
                    .columns(COLUMNS_MEDIUM)
//                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(graph))
//                    .textValidation(versionValidation)
            }
        }.expanded = true

    }
}
