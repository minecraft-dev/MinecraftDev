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

package com.demonwav.mcdev.creator.buildsystem

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.notifyCreatedProjectNotOpened
import com.demonwav.mcdev.creator.step.AbstractLongRunningStep
import com.demonwav.mcdev.creator.step.AbstractReformatFilesStep
import com.demonwav.mcdev.creator.step.FixedAssetsNewProjectWizardStep
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.GRADLE_WRAPPER_PROPERTIES
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.runGradleTask
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.demonwav.mcdev.util.virtualFileOrError
import com.intellij.execution.RunManager
import com.intellij.ide.ui.UISettings
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.StatusBarEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.project.open.canLinkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.service.project.open.linkAndSyncGradleProject

val DEFAULT_GRADLE_VERSION = SemanticVersion.release(8, 7)
val GRADLE_VERSION_KEY = Key.create<SemanticVersion>("mcdev.gradleVersion")

fun FixedAssetsNewProjectWizardStep.addGradleWrapperProperties(project: Project) {
    val gradleVersion = data.getUserData(GRADLE_VERSION_KEY) ?: DEFAULT_GRADLE_VERSION
    addTemplateProperties("GRADLE_WRAPPER_VERSION" to gradleVersion)
    addTemplates(project, "gradle/wrapper/gradle-wrapper.properties" to GRADLE_WRAPPER_PROPERTIES)
}

abstract class AbstractRunGradleTaskStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    abstract val task: String
    override val description get() = "Running Gradle task: '$task'"

    override fun perform(project: Project) {
        val outputDirectory = context.projectFileDirectory
        DumbService.getInstance(project).runWhenSmart {
            runGradleTask(project, Path.of(outputDirectory)) { settings ->
                settings.taskNames = listOf(task)
            }
        }
    }
}

class GradleWrapperStep(parent: NewProjectWizardStep) : AbstractRunGradleTaskStep(parent) {
    override val task = "wrapper"
}

abstract class AbstractPatchGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description
        get() = MCDevBundle("creator.step.gradle.patch_gradle.description")

    abstract fun patch(project: Project, gradleFiles: GradleFiles)

    protected fun addRepositories(project: Project, buildGradle: GradleFile?, repositories: List<BuildRepository>) {
        if (buildGradle == null || repositories.isEmpty()) {
            return
        }

        buildGradle.psi.runWriteAction {
            buildGradle.addRepositories(project, repositories)
        }
    }

    protected fun addDependencies(project: Project, buildGradle: GradleFile?, dependencies: List<BuildDependency>) {
        if (buildGradle == null || dependencies.isEmpty()) {
            return
        }

        buildGradle.psi.runWriteAction {
            buildGradle.addDependencies(project, dependencies)
        }
    }

    protected fun addPlugins(project: Project, buildGradle: GradleFile?, plugins: List<GradlePlugin>) {
        if (buildGradle == null || plugins.isEmpty()) {
            return
        }

        buildGradle.psi.runWriteAction {
            buildGradle.addPlugins(project, plugins)
        }
    }

    override fun perform(project: Project) {
        invokeAndWait {
            if (project.isDisposed || !project.isInitialized) {
                notifyCreatedProjectNotOpened()
                return@invokeAndWait
            }

            runWriteTask {
                val rootDir = VfsUtil.findFile(Path.of(context.projectFileDirectory), true)
                    ?: return@runWriteTask
                val gradleFiles = GradleFiles(project, rootDir)
                NonProjectFileWritingAccessProvider.disableChecksDuring {
                    patch(project, gradleFiles)
                    gradleFiles.commit()
                }
            }
        }
    }

    class GradleFiles(
        private val project: Project,
        private val rootDir: VirtualFile,
    ) {
        private val lazyBuildGradle = lazy {
            val file = rootDir.findChild("build.gradle") ?: rootDir.findChild("build.gradle.kts")
                ?: return@lazy null
            makeGradleFile(file)
        }
        private val lazySettingsGradle = lazy {
            val file = rootDir.findChild("settings.gradle") ?: rootDir.findChild("settings.gradle.kts")
                ?: return@lazy null
            makeGradleFile(file)
        }
        private val lazyGradleProperties = lazy {
            val file = rootDir.findChild("gradle.properties") ?: return@lazy null
            PsiManager.getInstance(project).findFile(file) as? PropertiesFile
        }

        val buildGradle by lazyBuildGradle
        val settingsGradle by lazySettingsGradle
        val gradleProperties by lazyGradleProperties

        private fun makeGradleFile(virtualFile: VirtualFile): GradleFile? {
            val psi = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
            return GradleFile.EP_NAME.extensions.mapFirstNotNull { it.createGradleFile(psi) }
        }

        fun commit() {
            val files = mutableListOf<PsiFile>()
            if (lazyBuildGradle.isInitialized()) {
                buildGradle?.psi?.let { files += it }
            }
            if (lazySettingsGradle.isInitialized()) {
                settingsGradle?.psi?.let { files += it }
            }
            if (lazyGradleProperties.isInitialized()) {
                (gradleProperties as? PsiFile)?.let { files += it }
            }

            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            val fileDocumentManager = FileDocumentManager.getInstance()
            for (file in files) {
                val document = psiDocumentManager.getDocument(file) ?: continue
                fileDocumentManager.saveDocument(document)
            }
        }
    }
}

open class GradleImportStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description
        get() = MCDevBundle("creator.step.gradle.import_gradle.description")

    open val additionalRunTasks = emptyList<String>()

    override fun perform(project: Project) {
        if (!project.isInitialized) {
            notifyCreatedProjectNotOpened()
            return
        }

        val rootDirectory = Path.of(context.projectFileDirectory)
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()

        // Tell IntelliJ to import this project
        rootDirectory.virtualFileOrError.refresh(false, true)

        val latch = CountDownLatch(1)

        invokeLater(project.disposed) {
            val path = rootDirectory.toAbsolutePath().toString()
            if (canLinkAndRefreshGradleProject(path, project, false)) {
                runBlocking {
                    linkAndSyncGradleProject(project, path)
                }
                showProgress(project)
            }

            StartupManager.getInstance(project).runAfterOpened {
                latch.countDown()
            }
        }

        // Set up the run config
        // Get the gradle external task type, this is what sets it as a gradle task
        addRunTaskConfiguration(project, rootDirectory, buildSystemProps, "build")
        for (tasks in additionalRunTasks) {
            addRunTaskConfiguration(project, rootDirectory, buildSystemProps, tasks)
        }

        if (!ApplicationManager.getApplication().isDispatchThread) {
            latch.await()
        }
    }

    private fun addRunTaskConfiguration(
        project: Project,
        rootDirectory: Path,
        buildSystemProps: BuildSystemPropertiesStep<*>,
        task: String,
    ) {
        val gradleType = GradleExternalTaskConfigurationType.getInstance()

        val runManager = RunManager.getInstance(project)
        val runConfigName = buildSystemProps.artifactId + ' ' + task

        val runConfiguration = GradleRunConfiguration(project, gradleType.factory, runConfigName)

        // Set relevant gradle values
        runConfiguration.settings.externalProjectPath = rootDirectory.toAbsolutePath().toString()
        runConfiguration.settings.executionName = runConfigName
        runConfiguration.settings.taskNames = listOf(task)

        runConfiguration.isAllowRunningInParallel = false

        val settings = runManager.createConfiguration(
            runConfiguration,
            gradleType.factory,
        )

        settings.isActivateToolWindowBeforeRun = true
        settings.storeInLocalWorkspace()

        runManager.addConfiguration(settings)
        if (runManager.selectedConfiguration == null) {
            runManager.selectedConfiguration = settings
        }
    }
}

class ReformatBuildGradleStep(parent: NewProjectWizardStep) : AbstractReformatFilesStep(parent) {
    override fun addFilesToReformat() {
        addFileToReformat("build.gradle")
        addFileToReformat("build.gradle.kts")
    }
}

// Show the background processes window for setup tasks
private fun showProgress(project: Project) {
    if (!UISettings.getInstance().showStatusBar || UISettings.getInstance().presentationMode) {
        return
    }

    val statusBar = WindowManager.getInstance().getStatusBar(project) as? StatusBarEx ?: return
    statusBar.isProcessWindowOpen = true
}

data class GradlePlugin(
    val id: String,
    val version: String? = null,
    val apply: Boolean = true,
)
