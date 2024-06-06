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
import com.demonwav.mcdev.creator.custom.finalizers.CreatorFinalizer
import com.demonwav.mcdev.creator.custom.providers.EmptyLoadedTemplate
import com.demonwav.mcdev.creator.custom.providers.LoadedTemplate
import com.demonwav.mcdev.creator.custom.providers.RecentTemplatesProvider
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.creator.custom.types.CreatorPropertyFactory
import com.demonwav.mcdev.creator.custom.types.ExternalCreatorProperty
import com.demonwav.mcdev.util.toTypedArray
import com.demonwav.mcdev.util.virtualFileOrError
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GitNewProjectWizardData
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.observable.util.or
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.application
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * The step to select a custom template repo.
 */
class CustomPlatformStep(
    parent: NewProjectWizardStep,
) : AbstractNewProjectWizardStep(parent) {

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

        initTemplates(null)

        templateProviderPlaceholder.component = templateProvider.setupUi(context, propertyGraph, provideTemplate)
    }

    private fun initTemplates(
        taskParentComponent: JComponent?
    ) {
        selectedTemplate = EmptyLoadedTemplate

        val task = object : Task.Modal(
            context.project,
            taskParentComponent,
            MCDevBundle("creator.step.generic.init_template_providers.message"),
            false
        ) {

            override fun run(indicator: ProgressIndicator) {
                if (project?.isDisposed == true) {
                    return
                }

                for (provider in templateProviders) {
                    indicator.text = provider.getLabel()
                    runCatching { provider.init(indicator) }
                        .getOrLogException(logger<CustomPlatformStep>())
                }
            }
        }

        ProgressManager.getInstance().run(task)
    }

    private fun loadTemplatesInBackground(
        provider: () -> Collection<LoadedTemplate>,
        taskParentComponent: JComponent?
    ) {
        selectedTemplate = EmptyLoadedTemplate

        val task = object : Task.WithResult<Collection<LoadedTemplate>, Exception>(
            context.project,
            taskParentComponent,
            MCDevBundle("creator.step.generic.init_template_providers.message"),
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
            template.descriptor.properties.orEmpty()
                .mapNotNull { setupProperty(it) }
                .sortedBy { (_, order) -> order }
                .map { it.first }
        } catch (t: Throwable) {
            if (t is ControlFlowException) {
                throw t
            }
            thisLogger().error(t)
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

        if (descriptor.hidden == true) {
            return null
        }

        val factory = Consumer<Panel> { panel -> prop.buildUi(panel, context) }
        val order = descriptor.order ?: 0
        return factory to order
    }

    override fun setupProject(project: Project) {
        val template = selectedTemplate
        if (template is EmptyLoadedTemplate) {
            return
        }

        if (templateProvider !is RecentTemplatesProvider) {
            RecentProjectTemplates.instance.addNewTemplate(templateProvider.javaClass.name, template)
        }

        val projectPath = context.projectDirectory
        val templateProperties = collectTemplateProperties()
        thisLogger().debug("Template properties: $templateProperties")

        val generatedFiles = mutableListOf<Pair<TemplateFile, VirtualFile>>()
        for (file in template.descriptor.files.orEmpty()) {
            if (file.condition != null &&
                !TemplateEvaluator.condition(templateProperties, file.condition).getOrElse { false }
            ) {
                continue
            }

            val relativeTemplate = TemplateEvaluator.template(templateProperties, file.template).getOrNull()
                ?: continue
            val relativeDest = TemplateEvaluator.template(templateProperties, file.destination).getOrNull()
                ?: continue

            try {
                val templateContents = template.loadTemplateContents(relativeTemplate)
                    ?: continue

                val destPath = projectPath.resolve(relativeDest).toAbsolutePath()
                if (!destPath.startsWith(projectPath)) {
                    // We want to make sure template files aren't 'escaping' the project directory
                    continue
                }

                val processedContent = TemplateEvaluator.template(templateProperties, templateContents)
                    .getOrLogException(thisLogger())
                    ?: continue

                destPath.parent.createDirectories()
                destPath.writeText(processedContent)

                val virtualFile = destPath.refreshAndFindVirtualFile()
                if (virtualFile != null) {
                    generatedFiles.add(file to virtualFile)
                } else {
                    thisLogger().warn("Could not find VirtualFile for file generated at $destPath (descriptor: $file)")
                }
            } catch (t: Throwable) {
                if (t is ControlFlowException) {
                    throw t
                }

                thisLogger().error("Failed to process template file $file", t)
            }
        }

        application.executeOnPooledThread {
            application.invokeLater({
                application.runWriteAction {
                    LocalFileSystem.getInstance().refresh(false)
                    // Apparently a module root is required for the reformat to work
                    setupTempRootModule(project, projectPath)
                }
                reformatFiles(project, generatedFiles)
                openFilesInEditor(project, generatedFiles)
            }, project.disposed)

            val finalizers = selectedTemplate.descriptor.finalizers
            if (!finalizers.isNullOrEmpty()) {
                CreatorFinalizer.executeAll(project, finalizers, templateProperties)
            }
        }
    }

    private fun setupTempRootModule(project: Project, projectPath: Path) {
        val modifiableModel = ModuleManager.getInstance(project).getModifiableModel()
        val module = modifiableModel.newNonPersistentModule("mcdev-temp-root", ModuleTypeId.JAVA_MODULE)
        val rootsModel = ModuleRootManager.getInstance(module).modifiableModel
        rootsModel.addContentEntry(projectPath.virtualFileOrError)
        rootsModel.commit()
        modifiableModel.commit()
    }

    private fun collectTemplateProperties(): MutableMap<String, Any?> {
        val into = mutableMapOf<String, Any?>()

        into.putAll(TemplateEvaluator.baseProperties)

        val gitData = data.getUserData(GitNewProjectWizardData.KEY)
        into["USE_GIT"] = gitData?.git == true

        return properties.mapValuesTo(into) { (_, prop) -> prop.get() }
    }

    private fun reformatFiles(
        project: Project,
        files: MutableList<Pair<TemplateFile, VirtualFile>>
    ) {
        val psiManager = PsiManager.getInstance(project)
        val psiFiles = files.asSequence()
            .filter { (desc, _) -> desc.reformat != false }
            .mapNotNull { (_, file) -> psiManager.findFile(file) }
        ReformatCodeProcessor(project, psiFiles.toTypedArray(), null, false).run()
    }

    private fun openFilesInEditor(
        project: Project,
        files: MutableList<Pair<TemplateFile, VirtualFile>>
    ) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val projectView = ProjectView.getInstance(project)
        for ((desc, file) in files) {
            if (desc.openInEditor == true) {
                fileEditorManager.openFile(file, true)
                projectView.select(null, file, false)
            }
        }
    }
}
