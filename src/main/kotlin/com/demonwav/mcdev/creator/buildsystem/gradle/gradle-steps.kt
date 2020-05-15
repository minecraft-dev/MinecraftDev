/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem.gradle

import com.demonwav.mcdev.creator.CreatorStep
import com.demonwav.mcdev.creator.CreatorStep.Companion.writeText
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.BuildSystemTemplate
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.util.asPrimitiveType
import com.demonwav.mcdev.util.findDeclaredField
import com.demonwav.mcdev.util.invokeDeclaredMethod
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.runGradleTask
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.demonwav.mcdev.util.virtualFileOrError
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.RunManager
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.openapi.wm.impl.IdeRootPane
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.GroovyLanguage
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression

class BasicGradleStep(
    private val project: Project,
    private val rootDirectory: Path,
    private val buildSystem: BuildSystem,
    private val gradleFiles: GradleFiles<String>
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        // We will do this ourselves
        ExternalProjectsManagerImpl.disableProjectWatcherAutoUpdate(project)

        val (_, gradleProp, settingsGradle) = setupGradleFiles(rootDirectory, gradleFiles)

        runWriteTask {
            val buildGradlePsi = addBuildGradleDependencies(project, buildSystem, gradleFiles.buildGradle)
            val psiManager = PsiManager.getInstance(project)
            psiManager.findDirectory(rootDirectory.virtualFileOrError)?.let { dir ->
                dir.findFile(buildGradlePsi.name)?.delete()
                val newFile = dir.add(buildGradlePsi) as? PsiFile ?: return@let
                ReformatCodeProcessor(newFile, false).run()
            }

            if (gradleProp != null && gradleFiles.gradleProperties != null) {
                writeText(gradleProp, gradleFiles.gradleProperties, psiManager)
            }
            if (settingsGradle != null && gradleFiles.settingsGradle != null) {
                writeText(settingsGradle, gradleFiles.settingsGradle, psiManager)
            }
        }
    }
}

data class GradleFiles<out T>(
    val buildGradle: T,
    val gradleProperties: T?,
    val settingsGradle: T?
)

fun setupGradleFiles(dir: Path, givenFiles: GradleFiles<String>): GradleFiles<Path> {
    return GradleFiles(
        dir.resolve("build.gradle"),
        givenFiles.gradleProperties?.let { dir.resolve("gradle.properties") },
        givenFiles.settingsGradle?.let { dir.resolve("settings.gradle") }
    ).apply {
        Files.createFile(buildGradle)
        gradleProperties?.let { Files.createFile(it) }
        settingsGradle?.let { Files.createFile(it) }
    }
}

fun addBuildGradleDependencies(project: Project, buildSystem: BuildSystem, text: String): PsiFile {
    val file = PsiFileFactory.getInstance(project).createFileFromText(GroovyLanguage, text)
    return file.runWriteAction {
        val fileName = "build.gradle"
        file.name = fileName

        val groovyFile = file as GroovyFile

        buildSystem.repositories.asSequence()
            .filter { it.buildSystems.contains(BuildSystemType.GRADLE) }
            .map { "maven {name = '${it.id}'\nurl = '${it.url}'\n}" }
            .toList()
            .let { reps ->
                if (reps.isNotEmpty()) {
                    createRepositoriesOrDependencies(project, groovyFile, "repositories", reps)
                }
            }

        buildSystem.dependencies.asSequence()
            .filter { it.gradleConfiguration != null }
            .map { "${it.gradleConfiguration} '${it.groupId}:${it.artifactId}:${it.version}'" }
            .toList()
            .let { deps ->
                if (buildSystem.dependencies.isNotEmpty()) {
                    createRepositoriesOrDependencies(project, groovyFile, "dependencies", deps)
                }
            }

        return@runWriteAction file
    }
}

private fun createRepositoriesOrDependencies(
    project: Project,
    file: GroovyFile,
    name: String,
    expressions: Iterable<String>
) {
    // Get the block so we can start working with it
    val block = getClosableBlockByName(file, name)
        ?: throw IllegalStateException("Failed to parse build.gradle files")

    // Create a super expression with all the expressions tied together
    val expressionText = expressions.joinToString("\n")

    // We can't create each expression and add them to the file...that won't work. Groovy requires a new line
    // from one method call expression to another, and there's no way to (easily) put whitespace in Psi because Psi is
    // stupid. So instead we make the whole thing as one big clump and insert it into the block.
    val fakeFile = GroovyPsiElementFactory.getInstance(project).createGroovyFile(expressionText, false, null)
    val last = block.children.last()
    block.addBefore(fakeFile, last)
}

private fun getClosableBlockByName(element: PsiElement, name: String) =
    element.children.asSequence()
        .filter {
            // We want to find the child which has a GrReferenceExpression with the right name
            it.children.any { child -> child is GrReferenceExpression && child.text == name }
        }.map {
            // We want to find the grandchild which is a GrClosable block
            it.children.mapNotNull { child -> child as? GrClosableBlock }.firstOrNull()
        }.filterNotNull()
        .firstOrNull()

class BasicGradleFinalizerStep(
    private val module: Module,
    private val rootDirectory: Path,
    private val buildSystem: BuildSystem
) : CreatorStep {
    private val project
        get() = module.project

    override fun runStep(indicator: ProgressIndicator) {
        // Tell IntelliJ to import this project
        rootDirectory.virtualFileOrError.refresh(false, true)
        @Suppress("UnstableApiUsage")
        linkAndRefreshGradleProject(rootDirectory.toAbsolutePath().toString(), project)

        invokeLater {
            showProgress(project)
        }

        // Set up the run config
        // Get the gradle external task type, this is what sets it as a gradle task
        val gradleType = GradleExternalTaskConfigurationType.getInstance()

        val runManager = RunManager.getInstance(project)
        val runConfigName = buildSystem.artifactId + " build"

        val runConfiguration = ExternalSystemRunConfiguration(
            GradleConstants.SYSTEM_ID,
            project,
            gradleType.configurationFactories[0],
            runConfigName
        )

        // Set relevant gradle values
        runConfiguration.settings.externalProjectPath = rootDirectory.toAbsolutePath().toString()
        runConfiguration.settings.executionName = runConfigName
        runConfiguration.settings.taskNames = listOf("build")

        runConfiguration.isAllowRunningInParallel = false

        val settings = runManager.createConfiguration(
            runConfiguration,
            GradleExternalTaskConfigurationType.getInstance().configurationFactories.first()
        )

        settings.isActivateToolWindowBeforeRun = true

        runManager.addConfiguration(settings, false)
        if (runManager.selectedConfiguration == null) {
            runManager.selectedConfiguration = settings
        }
    }
}

class GradleWrapperStep(
    private val project: Project,
    private val rootDirectory: Path,
    private val buildSystem: GradleBuildSystem
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val wrapperVersion = buildSystem.gradleVersion

        // Setup gradle wrapper
        // We'll write the properties file to ensure it sets up with the right version
        val wrapperDir = rootDirectory.resolve("gradle/wrapper")
        Files.createDirectories(wrapperDir)
        val wrapperProp = wrapperDir.resolve("gradle-wrapper.properties")

        val text = "distributionUrl=https\\://services.gradle.org/distributions/gradle-$wrapperVersion-bin.zip\n"

        Files.write(wrapperProp, text.toByteArray(Charsets.UTF_8), CREATE, WRITE, TRUNCATE_EXISTING)

        indicator.text = "Setting up Gradle Wrapper"
        indicator.text2 = "Running Gradle task: 'wrapper'"
        runGradleTask(project, rootDirectory) { settings ->
            settings.taskNames = listOf("wrapper")
        }
        indicator.text2 = null
    }
}

// Try and show the background setup tasks, but I don't know of a way to do this without all this reflection and
// it's extremely likely this can fail - just do nothing if it does
private fun showProgress(project: Project) {
    if (!UISettings.instance.showStatusBar || UISettings.instance.presentationMode) {
        return
    }
    @Suppress("UnstableApiUsage")
    val pane = WindowManagerEx.getInstanceEx().getFrame(project)?.rootPane as? IdeRootPane ?: return
    pane.findDeclaredField("myStatusBar")
        ?.findDeclaredField("myInfoAndProgressPanel")
        ?.invokeDeclaredMethod("openProcessPopup", arrayOf(Boolean::class.asPrimitiveType), arrayOf(true))
}

class GradleGitignoreStep(
    private val project: Project,
    private val rootDirectory: Path
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val gitignoreFile = rootDirectory.resolve(".gitignore")

        val fileText = BuildSystemTemplate.applyGradleGitignore(project)

        Files.write(gitignoreFile, fileText.toByteArray(Charsets.UTF_8), CREATE, WRITE, TRUNCATE_EXISTING)
    }
}
