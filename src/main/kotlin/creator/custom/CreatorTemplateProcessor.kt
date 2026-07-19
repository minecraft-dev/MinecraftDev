/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.creator.custom.types.CreatorPropertyFactory
import com.demonwav.mcdev.creator.custom.types.ExternalCreatorProperty
import com.demonwav.mcdev.util.toTypedArray
import com.demonwav.mcdev.util.virtualFileOrError
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.application
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface ExternalTemplatePropertyProvider {

    val projectNameProperty: GraphProperty<String>

    val useGit: Boolean
}

/**
 * Handles all the logic involved in making the creator UI and generating the project files.
 */
class CreatorTemplateProcessor(
    propertyGraph: PropertyGraph,
    wizardContext: WizardContext,
    scope: CoroutineScope,
    private val externalPropertyProvider: ExternalTemplatePropertyProvider,
) {

    var hasTemplateErrors: Boolean = true
        private set

    private var properties: MutableMap<String, CreatorProperty<*>> = mutableMapOf()
    var context: CreatorContext = CreatorContext(propertyGraph, properties, wizardContext, scope)

    fun initBuiltinProperties() {
        val projectNameProperty = externalPropertyProvider.projectNameProperty
        properties["PROJECT_NAME"] = ExternalCreatorProperty(
            context = context,
            graphProperty = projectNameProperty,
            valueType = String::class.java
        )
    }

    fun createOptionsPanel(template: LoadedTemplate): DialogPanel? {
        properties = mutableMapOf()
        context = context.copy(properties = properties)

        if (!template.isValid) {
            return null
        }

        initBuiltinProperties()

        return panel {
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

    fun setupTemplate(
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

            if (t is TemplateValidationException && application.isUnitTestMode) {
                // Rethrowing makes the exception properly catchable while not polluting the test outputs
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
                val group = if (descriptor.collapsible == false) {
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
                    group
                }

                val isVisible = CreatorProperty.setupVisibleProperty(context.graph, context.properties, reporter, descriptor.visible)
                group.visibleIf(isVisible)
            }

            val order = descriptor.order ?: 0
            return factory to order
        }

        if (descriptor.name in properties.keys) {
            reporter.fatal("Duplicate property name ${descriptor.name}")
        }

        val prop = CreatorPropertyFactory.createFromType(descriptor.type, descriptor, context, reporter)
            ?: reporter.fatal("Unknown template property type ${descriptor.type}")

        prop.setupProperty(reporter)

        properties[descriptor.name] = prop

        if (descriptor.visible == false) {
            return null
        }

        val factory = Consumer<Panel> { panel -> prop.buildUi(panel) }
        val order = descriptor.order ?: 0
        return factory to order
    }

    fun generateFiles(project: Project, template: LoadedTemplate) {
        if (template is EmptyLoadedTemplate) {
            return
        }

        val projectPath = context.wizardContext.projectDirectory
        val templateProperties = collectTemplateProperties()
        thisLogger().debug("Template properties: $templateProperties")

        val generatedFiles = mutableListOf<Pair<TemplateFile, Path>>()
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

                generatedFiles.add(file to destPath)
            } catch (t: Throwable) {
                if (t is ControlFlowException) {
                    throw t
                }

                thisLogger().error("Failed to process template file $file", t)
            }
        }

        val finalizeAction = suspend {
            WriteAction.runAndWait<Throwable> {
                LocalFileSystem.getInstance().refresh(false)
                // Apparently a module root is required for the reformat to work
                setupTempRootModule(project, projectPath)

                val genVirtualFiles = generatedFiles
                    .mapNotNullTo(mutableListOf()) {
                        it.first to (it.second.refreshAndFindVirtualFile() ?: return@mapNotNullTo null)
                    }
                reformatFiles(project, genVirtualFiles)
                openFilesInEditor(project, genVirtualFiles)
            }

            val finalizers = template.descriptor.finalizers
            if (!finalizers.isNullOrEmpty()) {
                CreatorFinalizer.executeAll(context.wizardContext, project, finalizers, templateProperties)
            }
        }
        if (context.wizardContext.isCreatingNewProject) {
            TemplateService.instance.registerFinalizerAction(project, finalizeAction)
        } else {
            context.scope.launch { finalizeAction() }
        }
    }

    private fun setupTempRootModule(project: Project, projectPath: Path) {
        val modifiableModel = ModuleManager.getInstance(project).getModifiableModel()
        val module = modifiableModel.newNonPersistentModule("mcdev-temp-root", "")
        val rootsModel = ModuleRootManager.getInstance(module).modifiableModel
        rootsModel.addContentEntry(projectPath.virtualFileOrError)
        rootsModel.commit()
        modifiableModel.commit()
    }

    private fun collectTemplateProperties(): MutableMap<String, Any?> {
        val into = mutableMapOf<String, Any?>()

        into.putAll(TemplateEvaluator.baseProperties)

        into["USE_GIT"] = externalPropertyProvider.useGit

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

        val processor = ReformatCodeProcessor(project, psiFiles.toTypedArray(), null, false)
        psiFiles.forEach(processor::setDoNotKeepLineBreaks)

        val insightSettings = CodeInsightSettings.getInstance()
        val oldSecondReformat = insightSettings.ENABLE_SECOND_REFORMAT
        insightSettings.ENABLE_SECOND_REFORMAT = true
        try {
            processor.run()
        } finally {
            insightSettings.ENABLE_SECOND_REFORMAT = oldSecondReformat
        }
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
