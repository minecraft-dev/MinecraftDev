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
import com.demonwav.mcdev.creator.custom.DerivationMethods
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.demonwav.mcdev.creator.custom.model.ClassFqn
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.fileTemplates.impl.CustomFileTemplate
import com.intellij.ide.starters.local.GeneratorTemplateFile
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.toStringProperty
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import com.intellij.util.io.readText
import java.nio.file.Path
import javax.swing.JComponent
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

    private val graphProperties = mutableMapOf<String, GraphProperty<*>>()
    private val templateProperties = mutableMapOf<String, () -> Any>()

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

    private fun createOptionsPanelInBackground(path: String, placeholder: Placeholder, taskParentComponent: JComponent?) {
        val task = object : Task.WithResult<DialogPanel?, Exception>(
            context.project,
            taskParentComponent,
            MCDevBundle("creator.step.generic.project_created.message"),
            false
        ) {

            override fun compute(indicator: ProgressIndicator): DialogPanel? {
                if (project?.isDisposed == true) {
                    return null
                }

                return doCreateOptionsPanel(path)
            }
        }

        placeholder.component = ProgressManager.getInstance().run(task)
    }

    private fun doCreateOptionsPanel(path: String): DialogPanel? {
        templateProperties.clear()
        descriptor = null
        val templateDescriptorPath = Path.of(path, ".mcdev.template.json")
        if (!templateDescriptorPath.exists()) {
            return null
        }

        return panel {
            val templateDescriptor = Gson().fromJson<TemplateDescriptor>(templateDescriptorPath.readText())
            descriptor = templateDescriptor
            for (prop in templateDescriptor.properties) {
                makeField(prop)
            }
        }
    }

    private fun Panel.makeField(prop: TemplateProperty) {
        when (prop.type) {
            "class_fqn" -> {
                val graphProp = propertyGraph.property("")
                if (prop.remember == true) {
                    graphProp.bindStorage(makeStorageKey(prop))
                }

                graphProperties[prop.name] = graphProp
                templateProperties[prop.name] = { ClassFqn(graphProp.get()) }

                row(MCDevBundle("creator.ui.custom.property.${prop.type}.label")) {
                    textField().bindText(graphProp).columns(COLUMNS_LARGE).enabled(prop.editable != false)
                }.visible(prop.hidden != true)
            }

            "boolean" -> {
                val graphProp = propertyGraph.property(prop.default as? Boolean ?: false)
                if (prop.remember == true) {
                    graphProp.bindBooleanStorage(makeStorageKey(prop))
                }

                graphProperties[prop.name] = graphProp
                templateProperties[prop.name] = { graphProp.get() }

                row(prop.label) {
                    checkBox("").bindSelected(graphProp).enabled(prop.editable != false)
                }.visible(prop.hidden != true)
            }

            "dropdown" -> {
                val defaultIndex = prop.default as? Int
                val defaultValue = defaultIndex?.let { prop.options.getOrNull(it) } ?: prop.options.first()
                val graphProp = propertyGraph.property(defaultValue)

                if (prop.remember == true && prop.options.all { it is String }) {
                    graphProp.toStringProperty { it }.bindStorage(makeStorageKey(prop))
                }

                graphProperties[prop.name] = graphProp
                templateProperties[prop.name] = { graphProp.get() }

                row(prop.label) {
                    comboBox(prop.options).bindItem(graphProp).enabled(prop.editable != false)
                }.visible(prop.hidden != true)
            }

            "textfield" -> {
                val graphProp = propertyGraph.property(prop.default as? String ?: "")
                if (prop.remember == true) {
                    graphProp.bindStorage(makeStorageKey(prop))
                }

                if (prop.derives != null) {
                    val parentProperty = graphProperties[prop.derives.from]
                    if (parentProperty == null) {
                        thisLogger().error("Unknown parent property '${prop.derives.from}'")
                        return
                    }

                    val method = DerivationMethods.methods[prop.derives.method]
                    if (method == null) {
                        thisLogger().error("Unknown derivation method '${prop.derives.method}'")
                        return
                    }

                    graphProp.set(method(parentProperty.get())?.toString() ?: prop.derives.default as String)

                    graphProp.dependsOn(parentProperty, prop.derives.whenModified != false) {
                        method(parentProperty.get())?.toString() ?: prop.derives.default as String
                    }
                }

                graphProperties[prop.name] = graphProp
                templateProperties[prop.name] = { graphProp.get() }

                row(prop.label) {
                    textField().bindText(graphProp).enabled(prop.editable != false)
                }.visible(prop.hidden != true)
            }

            else -> thisLogger().error("Unknown template property type ${prop.type}")
        }
    }

    private fun makeStorageKey(prop: TemplateProperty) =
        "${CustomPlatformStep::class.java.name}.property.${prop.name}.${prop.type}"

    override fun setupAssets(project: Project) {
        val descriptor = descriptor!!

        val rootPath = Path.of(path).absolute()

        val baseData = data.getUserData(NewProjectWizardBaseData.KEY) ?: return
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()

        assets.addTemplateProperties(
            "PROJECT_NAME" to baseData.name,
            "JAVA_VERSION" to javaVersion,
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "VERSION" to buildSystemProps.version,
        )

        templateProperties.mapValuesTo(assets.templateProperties) { (_, property) -> property() }

        thisLogger().debug("Template properties: $templateProperties")

        for (file in descriptor.files) {
            if (file.condition != null &&
                TemplateEvaluator.condition(assets.templateProperties, file.condition).getOrElse { false }
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

    class TypeFactory : PlatformTypeStep.Factory {
        override val name
            get() = MCDevBundle("creator.ui.platform.custom.name")

        override fun createStep(parent: PlatformTypeStep) = CustomPlatformStep(parent)
    }
}
