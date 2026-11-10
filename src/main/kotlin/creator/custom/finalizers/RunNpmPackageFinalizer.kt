/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.creator.custom.finalizers

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.CreatorContext
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.npm.NpmEntryPoint
import com.demonwav.mcdev.creator.custom.npm.NpmEnvironment
import com.demonwav.mcdev.creator.custom.npm.NpmPackageInfo
import com.demonwav.mcdev.creator.custom.npm.NpmPackageSource
import com.intellij.icons.AllIcons
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.MAX_LINE_LENGTH_WORD_WRAP
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.util.ui.NamedColorUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Installs or updates an npm package in the plugin-maintained [NpmEnvironment], then runs one of its executables or
 * scripts.
 *
 * The creator warns the user that the package will be run and who published it.
 *
 * Properties:
 * - `package`: the name of an npm package, without any version
 * - `version`: optional exact version or dist-tag of the npm package to use, defaults to `latest`
 * - `repository`: the URL of a git repository with a package at its root, to use instead of an npm package. It is
 *   cloned and built from source, so it doesn't need to be published to npm
 * - `build`: optional list of scripts from the repository's `package.json` to run once its dependencies are installed,
 *   in order to build it
 * - `command`: the name of the executable to run, it must be provided by the package
 * - `script`: a JavaScript file to run with Node.js instead of an executable, relative to the package's root
 * - `args`: optional list of arguments, each one is evaluated as a template and dropped if it evaluates to a blank
 *   string, which allows arguments to be conditional
 * - `workingDirectory`: optional path relative to the project directory to run the command in, defaults to the
 *   project directory
 * - `input`: optional text to write to the command's standard input, evaluated as a template
 */
class RunNpmPackageFinalizer : CreatorFinalizer {

    override fun validate(
        reporter: TemplateValidationReporter,
        properties: Map<String, Any>
    ) {
        val packageName = properties["package"]
        val repository = properties["repository"]
        when {
            packageName != null && repository != null ->
                reporter.error("Only one of 'package' or 'repository' is allowed")
            packageName == null && repository == null -> reporter.error("Missing 'package' or 'repository' string")
            packageName != null && packageName !is String -> reporter.error("'package' must be a string")
            repository != null && repository !is String -> reporter.error("'repository' must be a string")
            // It would be passed to git as an option otherwise
            repository is String && repository.startsWith("-") -> reporter.error("'repository' must be a git URL")
        }

        for (key in listOf("version", "workingDirectory", "input")) {
            val value = properties[key]
            if (value != null && value !is String) {
                reporter.error("'$key' must be a string")
            }
        }

        if (packageName == null && properties["version"] != null) {
            reporter.error("'version' can only be used with 'package'")
        }
        if (repository == null && properties["build"] != null) {
            reporter.error("'build' can only be used with 'repository'")
        }

        val command = properties["command"]
        val script = properties["script"]
        when {
            command != null && script != null -> reporter.error("Only one of 'command' or 'script' is allowed")
            command == null && script == null -> reporter.error("Missing 'command' or 'script' string")
            command != null && command !is String -> reporter.error("'command' must be a string")
            script != null && script !is String -> reporter.error("'script' must be a string")
        }

        for (key in listOf("args", "build")) {
            val value = properties[key]
            if (value != null && (value !is List<*> || value.any { it !is String })) {
                reporter.error("'$key' must be a list of strings")
            }
        }
    }

    override fun createUi(properties: Map<String, Any>): List<FinalizerUi> = listOf(
        NpmExecutableUi,
        NpmPackageTrustUi(properties.source, properties.entryPoint),
    )

    override suspend fun execute(
        context: WizardContext,
        project: Project,
        properties: Map<String, Any>,
        templateProperties: Map<String, Any?>
    ) {
        val source = properties.source

        @Suppress("UNCHECKED_CAST")
        val args = (properties["args"] as? List<String>).orEmpty()
            .map { TemplateEvaluator.template(templateProperties, it).getOrThrow() }
            .filter { it.isNotBlank() }

        val input = (properties["input"] as? String)
            ?.let { TemplateEvaluator.template(templateProperties, it).getOrThrow() }

        val projectDir = context.projectDirectory.toAbsolutePath().normalize()
        val workingDirectory = (properties["workingDirectory"] as? String)
            ?.let { TemplateEvaluator.template(templateProperties, it).getOrThrow() }
            ?.let { projectDir.resolve(it).normalize() }
            ?: projectDir
        if (!workingDirectory.startsWith(projectDir)) {
            // Don't let templates run commands outside the project directory
            throw IllegalArgumentException("Working directory $workingDirectory is outside of the project directory")
        }

        withBackgroundProgress(project, MCDevBundle("creator.step.npm.run.description", source.displayName)) {
            NpmEnvironment.runPackage(project, source, properties.entryPoint, args, workingDirectory, input)
        }

        // The command most likely created or modified files, make sure the following finalizers can see them
        LocalFileSystem.getInstance().refreshNioFiles(listOf(projectDir), false, true, null)
    }

    private val Map<String, Any>.source: NpmPackageSource
        get() {
            val repository = this["repository"] as? String
                ?: return NpmPackageSource.Registry(this["package"] as String, this["version"] as? String ?: "latest")
            @Suppress("UNCHECKED_CAST")
            return NpmPackageSource.Git(repository, (this["build"] as? List<String>).orEmpty())
        }

    private val Map<String, Any>.entryPoint: NpmEntryPoint
        get() = (this["command"] as? String)?.let(NpmEntryPoint::Command)
            ?: NpmEntryPoint.Script(this["script"] as String)

    /**
     * Lets the user choose which npm installation to use, and prevents the project from being created if none can be
     * found.
     */
    data object NpmExecutableUi : FinalizerUi {

        override fun buildUi(panel: Panel, context: CreatorContext) {
            val pathProperty = context.graph.property(NpmEnvironment.customNpmPath)
            val errorProperty = context.graph.property("")

            lateinit var textField: JBTextField
            panel.row(MCDevBundle("creator.ui.npm_path.label")) {
                val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                    .withTitle(MCDevBundle("creator.ui.npm_path.dialog.title"))
                textFieldWithBrowseButton(descriptor)
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(pathProperty)
                    .comment(MCDevBundle("creator.ui.npm_path.comment"))
                    .applyToComponent { textField = this.textField as JBTextField }
                    .validationOnApply {
                        val path = pathProperty.get()
                        val error = findError(NpmEnvironment.findNpm(path), path)
                            ?: return@validationOnApply null
                        // The wizard only outlines the field, focus it so that its error popup shows up in case the
                        // user missed the error below it
                        invokeLater(ModalityState.stateForComponent(textField)) { textField.requestFocusInWindow() }
                        error(error)
                    }
            }

            panel.row("") {
                icon(AllIcons.General.Error)
                    .gap(RightGap.SMALL)
                label("")
                    .bindText(errorProperty)
                    .applyToComponent { foreground = NamedColorUtil.getErrorForeground() }
            }.visibleIf(errorProperty.transform { it.isNotEmpty() })

            val scope = context.childScope("NpmExecutableUi")
            var job: Job? = null
            fun update(path: String) {
                job?.cancel()
                job = scope.launch(Dispatchers.IO) {
                    val npm = NpmEnvironment.findNpm(path)
                    withContext(context.uiContext) {
                        textField.emptyText.text = if (npm != null && path.isBlank()) {
                            MCDevBundle("creator.ui.npm_path.detected", npm)
                        } else {
                            MCDevBundle("creator.ui.npm_path.auto_detect")
                        }

                        errorProperty.set(findError(npm, path).orEmpty())
                    }
                }
            }

            pathProperty.afterChange { path ->
                NpmEnvironment.customNpmPath = path
                update(path)
            }
            update(pathProperty.get())
        }

        private fun findError(npm: String?, path: String): String? = when {
            npm != null -> null
            path.isBlank() -> MCDevBundle("creator.ui.npm_path.not_found")
            else -> MCDevBundle("creator.ui.npm_path.invalid")
        }
    }

    /**
     * Warns the user that the template will run code from a package, and who published that package.
     */
    data class NpmPackageTrustUi(val source: NpmPackageSource, val entryPoint: NpmEntryPoint) : FinalizerUi {

        override fun buildUi(panel: Panel, context: CreatorContext) {
            panel.row {
                icon(AllIcons.General.Warning)
                    .align(AlignY.TOP)
                    .gap(RightGap.SMALL)
                when (source) {
                    is NpmPackageSource.Git -> {
                        val warning = MCDevBundle(
                            "creator.ui.npm_trust.git_warning",
                            escape(source.url),
                            escape(entryPoint.displayName),
                        )
                        text(warning, MAX_LINE_LENGTH_WORD_WRAP)
                            .align(AlignX.FILL)
                    }
                    is NpmPackageSource.Registry -> {
                        val fetching = MCDevBundle("creator.ui.npm_trust.fetching")
                        val text = text(createText(source, fetching), MAX_LINE_LENGTH_WORD_WRAP)
                            .align(AlignX.FILL)
                        context.childScope("NpmPackageTrustUi").launch(Dispatchers.IO) {
                            val details = try {
                                describePublisher(NpmEnvironment.fetchPackageInfo(source))
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                logger<NpmPackageTrustUi>()
                                    .info("Failed to fetch information about npm package ${source.name}", e)
                                MCDevBundle("creator.ui.npm_trust.fetch_failed")
                            }

                            withContext(context.uiContext) {
                                text.component.text = createText(source, details)
                            }
                        }
                    }
                }
            }
        }

        private fun createText(source: NpmPackageSource.Registry, details: String): String =
            MCDevBundle("creator.ui.npm_trust.warning", escape(source.name), escape(entryPoint.displayName), details)

        private fun describePublisher(info: NpmPackageInfo): String {
            val parts = mutableListOf<String>()
            if (info.publisher != null) {
                parts += MCDevBundle(
                    "creator.ui.npm_trust.publisher",
                    escape(info.version ?: "?"),
                    escape(info.publisher),
                )
            }
            if (info.maintainers.isNotEmpty()) {
                parts += MCDevBundle("creator.ui.npm_trust.maintainers", escape(info.maintainers.joinToString(", ")))
            }
            if (info.repository != null) {
                parts += MCDevBundle("creator.ui.npm_trust.repository", escape(info.repository))
            }
            return parts.joinToString(" ").ifEmpty { MCDevBundle("creator.ui.npm_trust.fetch_failed") }
        }

        // Everything coming from the registry or the template is untrusted, make sure it can't inject any HTML
        private fun escape(text: String) = StringUtil.escapeXmlEntities(text)
    }
}
