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

package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.providers.EmptyLoadedTemplate
import com.demonwav.mcdev.creator.custom.providers.LoadedTemplate
import com.demonwav.mcdev.creator.custom.providers.RecentTemplatesProvider
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.creator.custom.types.CreatorPropertyFactory
import com.demonwav.mcdev.creator.custom.types.ExternalCreatorProperty
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.intellij.ide.fileTemplates.impl.CustomFileTemplate
import com.intellij.ide.starters.local.GeneratorTemplateFile
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.util.or
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * The step to select a custom template repo.
 */
class CustomPlatformStep(
    parent: NewProjectWizardStep,
) : AbstractLongRunningAssetsStep(parent) {

    override val description: String = MCDevBundle("creator.ui.custom.step.description")

    val templateProviders = TemplateProvider.getAll()

    val templateProviderProperty = propertyGraph.property<TemplateProvider>(templateProviders.first())
    var templateProvider by templateProviderProperty

    val availableTemplatesProperty = propertyGraph.property<Collection<LoadedTemplate>>(emptyList())
    var availableTemplates by availableTemplatesProperty
    lateinit var availableTemplatesSegmentedButton: SegmentedButton<LoadedTemplate>

    val selectedTemplateProperty = propertyGraph.property<LoadedTemplate>(EmptyLoadedTemplate)
    var selectedTemplate by selectedTemplateProperty

    private var properties = mutableMapOf<String, CreatorProperty<*>>()

    override fun setupUI(builder: Panel) {
        var taskParentComponent: JComponent? = null

        lateinit var templateProviderPlaceholder: Placeholder
        lateinit var templatePropertyPlaceholder: Placeholder

        builder.row(MCDevBundle("creator.ui.custom.provider.label")) {
            segmentedButton(templateProviders, TemplateProvider::getLabel, TemplateProvider::getTooltip)
                .bind(templateProviderProperty)
        }

        builder.row {
            templateProviderPlaceholder = placeholder()
        }

        val provideTemplate = Consumer<() -> Collection<LoadedTemplate>> { provider ->
            loadTemplatesInBackground(provider, taskParentComponent)
        }

        templateProviderProperty.afterChange { templateProvider ->
            templatePropertyPlaceholder.component = null
            availableTemplates = emptyList()
            availableTemplatesSegmentedButton.items(availableTemplates)
            templateProviderPlaceholder.component = templateProvider.setupUi(context, propertyGraph, provideTemplate)
        }

        builder.row(MCDevBundle("creator.ui.custom.templates.label")) {
            availableTemplatesSegmentedButton =
                segmentedButton(emptyList(), LoadedTemplate::label, LoadedTemplate::tooltip)
                    .bind(selectedTemplateProperty)
        }.visibleIf(
            availableTemplatesProperty.transform { it.size > 1 } or
                templateProviderProperty.transform { it is RecentTemplatesProvider }
        )

        availableTemplatesProperty.afterChange { newTemplates ->
            availableTemplatesSegmentedButton.items(newTemplates)
            templatePropertyPlaceholder.component = null
            selectedTemplate = EmptyLoadedTemplate
        }

        selectedTemplateProperty.afterChange { template ->
            createOptionsPanelInBackground(template, templatePropertyPlaceholder)
        }

        builder.row {
            templatePropertyPlaceholder = placeholder().align(AlignX.FILL)
        }.topGap(TopGap.SMALL)

        templateProviderPlaceholder.component = templateProvider.setupUi(context, propertyGraph, provideTemplate)
    }

    private fun loadTemplatesInBackground(
        provider: () -> Collection<LoadedTemplate>,
        taskParentComponent: JComponent?
    ) {
        selectedTemplate = EmptyLoadedTemplate

        val task = object : Task.WithResult<Collection<LoadedTemplate>, Exception>(
            context.project,
            taskParentComponent,
            MCDevBundle("creator.step.generic.project_created.message"),
            false
        ) {

            override fun compute(indicator: ProgressIndicator): Collection<LoadedTemplate> {
                if (project?.isDisposed == true) {
                    return emptyList()
                }

                return runCatching { provider() }
                    .getOrLogException(logger<CustomPlatformStep>())
                    ?: emptyList()
            }
        }

        val newTemplates = ProgressManager.getInstance().run(task)
        availableTemplates = newTemplates
        availableTemplatesSegmentedButton.items(newTemplates)
        availableTemplatesSegmentedButton.selectedItem = newTemplates.firstOrNull()
    }

    private fun createOptionsPanelInBackground(template: LoadedTemplate, placeholder: Placeholder) {
        properties = mutableMapOf()

        if (!template.isValid) {
            return
        }

        val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
            ?: return thisLogger().error("Could not find wizard base data")

        properties["PROJECT_NAME"] = ExternalCreatorProperty(propertyGraph, properties, baseData.nameProperty)

        placeholder.component = panel {
            for (uiFactory in setupTemplate(template)) {
                uiFactory.accept(this)
            }
        }
    }

    private fun setupTemplate(template: LoadedTemplate): List<Consumer<Panel>> {
        return try {
            template.descriptor.properties
                .mapNotNull { setupProperty(it) }
                .sortedBy { (_, order) -> order }
                .map { it.first }
        } catch (e: Throwable) {
            thisLogger().error(e)
            emptyList()
        }
    }

    private fun setupProperty(descriptor: TemplatePropertyDescriptor): Pair<Consumer<Panel>, Int>? {
        if (!descriptor.groupProperties.isNullOrEmpty()) {
            val childrenUiFactories = descriptor.groupProperties
                .mapNotNull(::setupProperty)
                .sortedBy { (_, order) -> order }
                .map { it.first }

            val factory = Consumer<Panel> { panel ->
                if (descriptor.collapsible == false) {
                    panel.group(descriptor.label) {
                        for (childFactory in childrenUiFactories) {
                            childFactory.accept(this@group)
                        }
                    }
                } else {
                    val group = panel.collapsibleGroup(descriptor.label) {
                        for (childFactory in childrenUiFactories) {
                            childFactory.accept(this@collapsibleGroup)
                        }
                    }

                    group.expanded = descriptor.default as? Boolean ?: false
                }
            }

            val order = descriptor.order ?: 0
            return factory to order
        }

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
        val template = selectedTemplate
        if (template is EmptyLoadedTemplate) {
            return
        }

        if (templateProvider !is RecentTemplatesProvider) {
            RecentProjectTemplates.instance.addNewTemplate(templateProvider.javaClass.name, template)
        }

        val descriptor = template.descriptor

        collectTemplateProperties(assets.templateProperties)

        thisLogger().debug("Template properties: ${assets.templateProperties}")

        val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
            ?: return thisLogger().error("Could not find wizard base data")
        val projectPath = Path.of(baseData.path)

        for (file in descriptor.files) {
            if (file.condition != null &&
                !TemplateEvaluator.condition(assets.templateProperties, file.condition).getOrElse { false }
            ) {
                continue
            }

            val relativeTemplate = TemplateEvaluator.template(assets.templateProperties, file.template).getOrNull()
                ?: continue
            val relativeDest = TemplateEvaluator.template(assets.templateProperties, file.destination).getOrNull()
                ?: continue

            val templateContents = template.loadTemplateContents(relativeTemplate)
                ?: continue

            val destPath = projectPath.resolve(relativeDest).toAbsolutePath()
            if (!destPath.startsWith(projectPath)) {
                // We want to make sure template files aren't 'escaping' the project directory
                continue
            }

            val fileName = destPath.fileName.toString().removeSuffix(".ft")
            val baseFileName = FileUtilRt.getNameWithoutExtension(fileName)
            val extension = FileUtilRt.getExtension(fileName)
            val fileTemplate = CustomFileTemplate(baseFileName, extension)
            fileTemplate.text = templateContents
            assets.addAssets(GeneratorTemplateFile(projectPath.relativize(destPath).toString(), fileTemplate))
        }
    }

    private fun collectTemplateProperties(into: MutableMap<String, Any?> = mutableMapOf()): MutableMap<String, Any?> {
        return properties.mapValuesTo(into) { (_, prop) -> prop.get() }
    }
}
