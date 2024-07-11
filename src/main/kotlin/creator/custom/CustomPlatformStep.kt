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

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.finalizers.CreatorFinalizer
import com.demonwav.mcdev.creator.custom.providers.EmptyLoadedTemplate
import com.demonwav.mcdev.creator.custom.providers.LoadedTemplate
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.creator.custom.types.CreatorPropertyFactory
import com.demonwav.mcdev.creator.custom.types.ExternalCreatorProperty
import com.demonwav.mcdev.creator.modalityState
import com.demonwav.mcdev.util.toTypedArray
import com.demonwav.mcdev.util.virtualFileOrError
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GitNewProjectWizardData
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.application
import com.intellij.util.ui.AsyncProcessIcon
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JLabel
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

    val templateRepos = MinecraftSettings.instance.creatorTemplateRepos

    val templateRepoProperty = propertyGraph.property<MinecraftSettings.TemplateRepo>(
        templateRepos.firstOrNull() ?: MinecraftSettings.TemplateRepo.makeBuiltinRepo()
    )
    var templateRepo by templateRepoProperty

    val availableGroupsProperty = propertyGraph.property<Collection<String>>(emptyList())
    var availableGroups by availableGroupsProperty
    val availableTemplatesProperty = propertyGraph.property<Collection<LoadedTemplate>>(emptyList())
    var availableTemplates by availableTemplatesProperty
    lateinit var availableGroupsSegmentedButton: SegmentedButton<String>
    lateinit var availableTemplatesSegmentedButton: SegmentedButton<LoadedTemplate>

    val selectedGroupProperty = propertyGraph.property<String>("")
    var selectedGroup by selectedGroupProperty
    val selectedTemplateProperty = propertyGraph.property<LoadedTemplate>(EmptyLoadedTemplate)
    var selectedTemplate by selectedTemplateProperty

    val templateProvidersLoadingProperty = propertyGraph.property<Boolean>(true)
    val templateProvidersTextProperty = propertyGraph.property("")
    val templateProvidersText2Property = propertyGraph.property("")
    lateinit var templateProvidersProcessIcon: Cell<AsyncProcessIcon>

    val templateLoadingProperty = propertyGraph.property<Boolean>(true)
    val templateLoadingTextProperty = propertyGraph.property<String>("")
    val templateLoadingText2Property = propertyGraph.property<String>("")
    lateinit var templatePropertiesProcessIcon: Cell<AsyncProcessIcon>
    lateinit var noTemplatesAvailable: Cell<JLabel>
    var templateLoadingIndicator: ProgressIndicator? = null

    private var hasTemplateErrors: Boolean = true

    private var properties = mutableMapOf<String, CreatorProperty<*>>()

    override fun setupUI(builder: Panel) {
        lateinit var templatePropertyPlaceholder: Placeholder

        builder.row(MCDevBundle("creator.ui.custom.repos.label")) {
            segmentedButton(templateRepos) { it.name }
                .bind(templateRepoProperty)
        }.visible(templateRepos.size > 1)

        builder.row {
            templateProvidersProcessIcon =
                cell(AsyncProcessIcon("TemplateProviders init"))
                    .visibleIf(templateProvidersLoadingProperty)
            label(MCDevBundle("creator.step.generic.init_template_providers.message"))
                .visibleIf(templateProvidersLoadingProperty)
            label("")
                .bindText(templateProvidersTextProperty)
                .visibleIf(templateProvidersLoadingProperty)
            label("")
                .bindText(templateProvidersText2Property)
                .visibleIf(templateProvidersLoadingProperty)
        }

        templateRepoProperty.afterChange { templateRepo ->
            templatePropertyPlaceholder.component = null
            availableTemplates = emptyList()
            loadTemplatesInBackground {
                val provider = TemplateProvider.get(templateRepo.provider)
                provider?.loadTemplates(context, templateRepo).orEmpty()
            }
        }

        builder.row(MCDevBundle("creator.ui.custom.groups.label")) {
            availableGroupsSegmentedButton =
                segmentedButton(emptyList<String>(), String::toString)
                    .bind(selectedGroupProperty)
        }.visibleIf(
            availableGroupsProperty.transform { it.size > 1 }
        )

        builder.row(MCDevBundle("creator.ui.custom.templates.label")) {
            availableTemplatesSegmentedButton =
                segmentedButton(emptyList(), LoadedTemplate::label, LoadedTemplate::tooltip)
                    .bind(selectedTemplateProperty)
                    .validation {
                        addApplyRule("", condition = ::hasTemplateErrors)
                    }
        }.visibleIf(
            availableTemplatesProperty.transform { it.size > 1 }
        )

        availableTemplatesProperty.afterChange { newTemplates ->
            val groups = newTemplates.mapTo(linkedSetOf()) { it.descriptor.translatedGroup }
            availableGroupsSegmentedButton.items(groups)
            // availableGroupsSegmentedButton.visible(groups.size > 1)
            availableGroups = groups
            selectedGroup = groups.firstOrNull() ?: "empty"
        }

        selectedGroupProperty.afterChange { group ->
            val templates = availableTemplates.filter { it.descriptor.translatedGroup == group }
            availableTemplatesSegmentedButton.items(templates)
            // Force visiblity because the component might become hidden and not show up again
            //  when the segmented button switches between dropdown and buttons
            availableTemplatesSegmentedButton.visible(true)
            templatePropertyPlaceholder.component = null
            selectedTemplate = templates.firstOrNull() ?: EmptyLoadedTemplate
        }

        selectedTemplateProperty.afterChange { template ->
            createOptionsPanelInBackground(template, templatePropertyPlaceholder)
        }

        builder.row {
            templatePropertiesProcessIcon =
                cell(AsyncProcessIcon("Templates loading"))
                    .visibleIf(templateLoadingProperty)
            label(MCDevBundle("creator.step.generic.load_template.message"))
                .visibleIf(templateLoadingProperty)
            label("")
                .bindText(templateLoadingTextProperty)
                .visibleIf(templateLoadingProperty)
            label("")
                .bindText(templateLoadingText2Property)
                .visibleIf(templateLoadingProperty)
            noTemplatesAvailable = label(MCDevBundle("creator.step.generic.no_templates_available.message"))
                .visible(false)
                .apply { component.foreground = JBColor.RED }
            templatePropertyPlaceholder = placeholder().align(AlignX.FILL)
        }.topGap(TopGap.SMALL)

        initTemplates()
    }

    private fun initTemplates() {
        selectedTemplate = EmptyLoadedTemplate

        val task = object : Task.Backgroundable(
            context.project,
            MCDevBundle("creator.step.generic.init_template_providers.message"),
            true,
            ALWAYS_BACKGROUND,
        ) {

            override fun run(indicator: ProgressIndicator) {
                if (project?.isDisposed == true) {
                    return
                }

                application.invokeAndWait({
                    ProgressManager.checkCanceled()
                    templateProvidersLoadingProperty.set(true)
                    VirtualFileManager.getInstance().syncRefresh()
                }, context.modalityState)

                for ((providerKey, repos) in templateRepos.groupBy { it.provider }) {
                    ProgressManager.checkCanceled()
                    val provider = TemplateProvider.get(providerKey)
                        ?: continue
                    indicator.text = provider.label
                    runCatching { provider.init(indicator, repos) }
                        .getOrLogException(logger<CustomPlatformStep>())
                }

                ProgressManager.checkCanceled()
                application.invokeAndWait({
                    ProgressManager.checkCanceled()
                    templateProvidersLoadingProperty.set(false)
                    // Force refresh to trigger template loading
                    templateRepoProperty.set(templateRepo)
                }, context.modalityState)
            }
        }

        val indicator = CreatorProgressIndicator(
            templateProvidersLoadingProperty,
            templateProvidersTextProperty,
            templateProvidersText2Property
        )
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator)
    }

    private fun loadTemplatesInBackground(provider: () -> Collection<LoadedTemplate>) {
        selectedTemplate = EmptyLoadedTemplate

        val task = object : Task.Backgroundable(
            context.project,
            MCDevBundle("creator.step.generic.load_template.message"),
            true,
            ALWAYS_BACKGROUND,
        ) {

            override fun run(indicator: ProgressIndicator) {
                if (project?.isDisposed == true) {
                    return
                }

                application.invokeAndWait({
                    ProgressManager.checkCanceled()
                    templateLoadingProperty.set(true)
                    VirtualFileManager.getInstance().syncRefresh()
                }, context.modalityState)

                ProgressManager.checkCanceled()
                val newTemplates = runCatching { provider() }
                    .getOrLogException(logger<CustomPlatformStep>())
                    ?: emptyList()

                ProgressManager.checkCanceled()
                application.invokeAndWait({
                    ProgressManager.checkCanceled()
                    templateLoadingProperty.set(false)
                    noTemplatesAvailable.visible(newTemplates.isEmpty())
                    availableTemplates = newTemplates
                }, context.modalityState)
            }
        }

        templateLoadingIndicator?.cancel()

        val indicator = CreatorProgressIndicator(
            templateLoadingProperty,
            templateLoadingTextProperty,
            templateLoadingText2Property
        )
        templateLoadingIndicator = indicator
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator)
    }

    private fun createOptionsPanelInBackground(template: LoadedTemplate, placeholder: Placeholder) {
        properties = mutableMapOf()

        if (!template.isValid) {
            return
        }

        val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
            ?: return thisLogger().error("Could not find wizard base data")

        properties["PROJECT_NAME"] = ExternalCreatorProperty(
            graph = propertyGraph,
            properties = properties,
            graphProperty = baseData.nameProperty,
            valueType = String::class.java
        )

        placeholder.component = panel {
            val reporter = TemplateValidationReporterImpl()
            val uiFactories = setupTemplate(template, reporter)
            if (uiFactories.isEmpty() && !reporter.hasErrors) {
                row {
                    label(MCDevBundle("creator.ui.warn.no_properties"))
                        .component.foreground = JBColor.YELLOW
                }
            } else {
                hasTemplateErrors = reporter.hasErrors
                reporter.display(this)

                if (!reporter.hasErrors) {
                    for (uiFactory in uiFactories) {
                        uiFactory.accept(this)
                    }
                }
            }
        }
    }

    private fun setupTemplate(
        template: LoadedTemplate,
        reporter: TemplateValidationReporterImpl
    ): List<Consumer<Panel>> {
        return try {
            val properties = template.descriptor.properties.orEmpty()
                .mapNotNull {
                    reporter.subject = it.name
                    setupProperty(it, reporter)
                }
                .sortedBy { (_, order) -> order }
                .map { it.first }

            val finalizers = template.descriptor.finalizers
            if (finalizers != null) {
                CreatorFinalizer.validateAll(reporter, finalizers)
            }

            properties
        } catch (t: Throwable) {
            if (t is ControlFlowException) {
                throw t
            }

            thisLogger().error(
                "Unexpected error during template setup",
                t,
                template.label,
                template.descriptor.toString()
            )

            emptyList()
        } finally {
            reporter.subject = null
        }
    }

    private fun setupProperty(
        descriptor: TemplatePropertyDescriptor,
        reporter: TemplateValidationReporter
    ): Pair<Consumer<Panel>, Int>? {
        if (!descriptor.groupProperties.isNullOrEmpty()) {
            val childrenUiFactories = descriptor.groupProperties
                .mapNotNull { setupProperty(it, reporter) }
                .sortedBy { (_, order) -> order }
                .map { it.first }

            val factory = Consumer<Panel> { panel ->
                val label = descriptor.translatedLabel
                if (descriptor.collapsible == false) {
                    panel.group(label) {
                        for (childFactory in childrenUiFactories) {
                            childFactory.accept(this@group)
                        }
                    }
                } else {
                    val group = panel.collapsibleGroup(label) {
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
            reporter.fatal("Duplicate property name ${descriptor.name}")
        }

        val prop = CreatorPropertyFactory.createFromType(descriptor.type, descriptor, propertyGraph, properties)
        if (prop == null) {
            reporter.fatal("Unknown template property type ${descriptor.type}")
        }

        prop.setupProperty(reporter)

        properties[descriptor.name] = prop

        if (descriptor.visible == false) {
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

                var fileTemplateProperties = templateProperties
                if (file.properties != null) {
                    fileTemplateProperties = templateProperties.toMutableMap()
                    fileTemplateProperties.putAll(file.properties)
                }

                val processedContent = TemplateEvaluator.template(fileTemplateProperties, templateContents)
                    .onFailure { t ->
                        val attachment = Attachment(relativeTemplate, templateContents)
                        thisLogger().error("Failed evaluate template '$relativeTemplate'", t, attachment)
                    }
                    .getOrNull()
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
                CreatorFinalizer.executeAll(context, finalizers, templateProperties)
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
