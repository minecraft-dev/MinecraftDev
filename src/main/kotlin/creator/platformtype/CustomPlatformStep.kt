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

package com.demonwav.mcdev.creator.platformtype

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.demonwav.mcdev.creator.custom.types.BooleanPropertyType
import com.demonwav.mcdev.creator.custom.types.ClassFqnPropertyType
import com.demonwav.mcdev.creator.custom.types.JdkPropertyType
import com.demonwav.mcdev.creator.custom.types.PropertyType
import com.demonwav.mcdev.creator.custom.types.SemanticVersionPropertyType
import com.demonwav.mcdev.creator.custom.types.StringPropertyType
import com.demonwav.mcdev.creator.custom.types.creator.custom.types.IntegerPropertyType
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.fileTemplates.impl.CustomFileTemplate
import com.intellij.ide.starters.local.GeneratorTemplateFile
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import com.intellij.util.io.readText
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.emptyList
import kotlin.collections.filterIsInstance
import kotlin.collections.isNullOrEmpty
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.mapOf
import kotlin.collections.mapValuesTo
import kotlin.collections.mutableMapOf
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.io.path.absolute
import kotlin.io.path.exists

/**
 * The step to select a custom template repo.
 */
class CustomPlatformStep(
    parent: PlatformTypeStep,
) : AbstractLongRunningAssetsStep(parent) {

    override val description: String = MCDevBundle("creator.ui.custom.step.description")

    val pathProperty = propertyGraph.property("").apply {
        bindStorage("${javaClass.name}.path")
    }
    var path by pathProperty

    val descriptorProperty = propertyGraph.property<TemplateDescriptor?>(null)
    var descriptor by descriptorProperty

    private val properties = mutableMapOf<String, CreatorProperty<*>>()

    override fun setupUI(builder: Panel) {
        var taskParentComponent: JComponent? = null
        builder.row(MCDevBundle("creator.ui.custom.path.label")) {
            val pathChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                description = MCDevBundle("creator.ui.custom.path.dialog.description")
            }
            textFieldWithBrowseButton(
                MCDevBundle("creator.ui.custom.path.dialog.title"),
                context.project,
                pathChooserDescriptor
            ).align(AlignX.FILL)
                .columns(COLUMNS_LARGE)
                .bindText(pathProperty)
                .textValidation(validationErrorIf(MCDevBundle("creator.validation.custom.path_not_a_directory")) { value ->
                    val file = kotlin.runCatching {
                        VirtualFileManager.getInstance().findFileByNioPath(Path.of(value))
                    }.getOrNull()
                    file == null || !file.isDirectory
                })
                .also { taskParentComponent = it.component }
        }

        builder.row {
            val placeholder = placeholder().align(AlignX.FILL)
            createOptionsPanelInBackground(path, placeholder, taskParentComponent)
            pathProperty.afterChange { path ->
                createOptionsPanelInBackground(path, placeholder, taskParentComponent)
            }
        }
    }

    private fun createOptionsPanelInBackground(
        path: String,
        placeholder: Placeholder,
        taskParentComponent: JComponent?
    ) {
        properties.clear()
        descriptor = null

        val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
            ?: return thisLogger().error("Could not find wizard base data")
//        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
//
//        properties["GROUP_ID"] = CreatorProperty(buildSystemProps.groupIdProperty, StringPropertyType())
//        properties["ARTIFACT_ID"] = CreatorProperty(buildSystemProps.artifactIdProperty, StringPropertyType())
//        properties["VERSION"] = CreatorProperty(buildSystemProps.versionProperty, StringPropertyType())

        properties["PROJECT_NAME"] = CreatorProperty(baseData.nameProperty, StringPropertyType())

        // TODO remove
        properties["GROUP_ID"] = CreatorProperty(propertyGraph.property("io.github.rednesto"), StringPropertyType())

        val task = object : Task.WithResult<List<Consumer<Panel>>, Exception>(
            context.project,
            taskParentComponent,
            MCDevBundle("creator.step.generic.project_created.message"),
            false
        ) {

            override fun compute(indicator: ProgressIndicator): List<Consumer<Panel>> {
                if (project?.isDisposed == true) {
                    return emptyList()
                }

                return setupTemplate(path)
            }
        }

        placeholder.component = panel {
            for (uiFactory in ProgressManager.getInstance().run(task)) {
                uiFactory.accept(this)
            }
        }
    }

    private fun setupTemplate(path: String): List<Consumer<Panel>> {
        val templateDescriptorPath = Path.of(path, ".mcdev.template.json")
        if (!templateDescriptorPath.exists()) {
            return emptyList()
        }

        val templateDescriptor = Gson().fromJson<TemplateDescriptor>(templateDescriptorPath.readText())
        descriptor = templateDescriptor
        return templateDescriptor.properties.mapNotNull { makeField(it) }
    }

    private fun makeField(prop: TemplateProperty): Consumer<Panel>? {
        if (prop.name in properties.keys) {
            thisLogger().error("Duplicate property name ${prop.name}")
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val type = when (prop.type) {
            "string" -> StringPropertyType()
            "integer" -> IntegerPropertyType()
            "boolean" -> BooleanPropertyType()
            "class_fqn" -> ClassFqnPropertyType()
            "semantic_version" -> SemanticVersionPropertyType()
            "jdk" -> JdkPropertyType()
            else -> {
                thisLogger().error("Unknown template property type ${prop.type}")
                return null
            }
        } as PropertyType<Any?>

        val isDropdown = !prop.options.isNullOrEmpty()
        val options = prop.options?.filterIsInstance<String>()?.map(type::deserialize) ?: emptyList()
        val defaultOptionIndex = if (isDropdown) prop.default as? Int ?: 0 else null
        val defaultValue =
            type.createDefaultValue(if (isDropdown) prop.options!![defaultOptionIndex!!] else prop.default)
        val graphProp = propertyGraph.property(defaultValue)

        if (prop.remember != false && prop.derives == null) {
            type.toStringProperty(graphProp).bindStorage(makeStorageKey(prop))
        }

        if (prop.derives != null) {
            val parents = prop.derives.parents
                ?: run {
                    thisLogger().error("No parents specified in derivation of property '${prop.name}'")
                    return null
                }
            for (parent in parents) {
                if (!properties.containsKey(parent)) {
                    thisLogger().error("Unknown parent property '${parent}' in derivation of property '${prop.name}'")
                    return null
                }
            }

            fun collectParentValues(): List<Any?> = parents.map { properties[it]!!.graphProperty.get() }

            graphProp.set(type.derive(graphProp, collectParentValues(), properties, prop.derives))
            for (parent in parents) {
                val parentProperty = properties[parent]!!
                graphProp.dependsOn(parentProperty.graphProperty, prop.derives.whenModified != false) {
                    type.derive(graphProp, collectParentValues(), properties, prop.derives)
                }
            }
        }

        if (prop.inheritFrom != null) {
            val parentProperty = properties[prop.inheritFrom]
                ?: run {
                    thisLogger().error("Unknown parent property '${prop.inheritFrom}' in derivation of property '${prop.name}'")
                    return null
                }

            graphProp.set(parentProperty.graphProperty.get())
            graphProp.dependsOn(parentProperty.graphProperty, true) { parentProperty.graphProperty.get() }
        }

        properties[prop.name] = CreatorProperty(graphProp, type)

        return Consumer { panel ->
            if (isDropdown) {
                panel.row(prop.label) {
                    comboBox(options)
                        .bindItem(graphProp)
                        .enabled(prop.editable != false)
                        .also { ComboboxSpeedSearch.installOn(it.component) }
                }.visible(prop.hidden != false)
            } else {
                with(panel) { with(type) { buildUi(context, graphProp, prop) } }
            }
        }
    }

    private fun makeStorageKey(prop: TemplateProperty) =
        "${CustomPlatformStep::class.java.name}.property.${prop.name}.${prop.type}"

    override fun setupAssets(project: Project) {
        val descriptor = descriptor!!

        val rootPath = Path.of(path).absolute()

        collectTemplateProperties(assets.templateProperties)

        thisLogger().debug("Template properties: ${assets.templateProperties}")

        for (file in descriptor.files) {
            if (file.condition != null &&
                !TemplateEvaluator.condition(assets.templateProperties, file.condition).getOrElse { false }
            ) {
                continue
            }

            val relativeTemplate = TemplateEvaluator.template(assets.templateProperties, file.template).getOrNull()
                ?: continue
            val templatePath = rootPath.resolve(relativeTemplate).toAbsolutePath()
            if (!templatePath.startsWith(rootPath)) {
                continue
            }

            val relativeDest = TemplateEvaluator.template(assets.templateProperties, file.destination).getOrNull()
                ?: continue
            val destPath = rootPath.resolve(relativeDest).toAbsolutePath()
            if (!destPath.startsWith(rootPath)) {
                // We want to make sure template files aren't 'escaping' the project directory
                continue
            }

            val fileName = destPath.fileName.toString().removeSuffix(".ft")
            val baseFileName = FileUtilRt.getNameWithoutExtension(fileName)
            val extension = FileUtilRt.getExtension(fileName)
            val template = CustomFileTemplate(baseFileName, extension)
            template.text = templatePath.readText()
            assets.addAssets(GeneratorTemplateFile(rootPath.relativize(destPath).toString(), template))
        }
    }

    private fun collectTemplateProperties(into: MutableMap<String, Any?> = mutableMapOf()): MutableMap<String, Any?> {
        val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
            ?: return into.also { thisLogger().error("Could not find wizard base data") }
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()

        into += mapOf(
            "PROJECT_NAME" to baseData.name,
            "JAVA_VERSION" to javaVersion,
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "VERSION" to buildSystemProps.version,
        )

        return properties.mapValuesTo(into) { (_, prop) -> prop.graphProperty.get() }
    }

    class TypeFactory : PlatformTypeStep.Factory {
        override val name
            get() = MCDevBundle("creator.ui.platform.custom.name")

        override fun createStep(parent: PlatformTypeStep) = CustomPlatformStep(parent)
    }
}
