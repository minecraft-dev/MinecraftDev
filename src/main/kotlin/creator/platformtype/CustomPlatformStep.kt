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
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.types.BooleanCreatorProperty
import com.demonwav.mcdev.creator.custom.types.BuildSystemCoordinatesCreatorProperty
import com.demonwav.mcdev.creator.custom.types.ClassFqnCreatorProperty
import com.demonwav.mcdev.creator.custom.types.JdkCreatorProperty
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.creator.custom.types.CreatorPropertyFactory
import com.demonwav.mcdev.creator.custom.types.ExternalCreatorProperty
import com.demonwav.mcdev.creator.custom.types.SemanticVersionCreatorProperty
import com.demonwav.mcdev.creator.custom.types.StringCreatorProperty
import com.demonwav.mcdev.creator.custom.types.IntegerCreatorProperty
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.fileTemplates.impl.CustomFileTemplate
import com.intellij.ide.starters.local.GeneratorTemplateFile
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
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

    private var properties = mutableMapOf<String, CreatorProperty<*>>()

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
        properties = mutableMapOf()
        descriptor = null

        val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
            ?: return thisLogger().error("Could not find wizard base data")

        properties["PROJECT_NAME"] = ExternalCreatorProperty(propertyGraph, properties, baseData.nameProperty)

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
        return templateDescriptor.properties
            .mapNotNull { setupProperty(it) }
            .sortedBy { (_, order) -> order }
            .map { it.first }
    }

    private fun setupProperty(descriptor: TemplatePropertyDescriptor): Pair<Consumer<Panel>, Int>? {
        if (descriptor.name in properties.keys) {
            thisLogger().error("Duplicate property name ${descriptor.name}")
            return null
        }

        val prop = CreatorPropertyFactory.createFromType(descriptor.type, descriptor, propertyGraph, properties)
        if (prop == null) {
            thisLogger().error("Unknown template property type ${descriptor.type}")
            return null
        }

        prop.setupProperty()

        properties[descriptor.name] = prop

        val factory = Consumer<Panel> { panel -> prop.buildUi(panel, context) }
        val order = descriptor.order ?: 0
        return factory to order
    }

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
