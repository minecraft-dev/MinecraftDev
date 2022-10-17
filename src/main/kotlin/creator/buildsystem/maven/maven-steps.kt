/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem.maven

import com.demonwav.mcdev.creator.CreatorStep
import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.BuildSystemTemplate
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.DirectorySet
import com.demonwav.mcdev.creator.getVersionJson
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.demonwav.mcdev.util.virtualFile
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.RunManager
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import kotlinx.coroutines.runBlocking
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.project.MavenProjectsManager

typealias MavenStepFunc = (BasicMavenStep, MavenDomProjectModel, XmlTag) -> Unit

class BasicMavenStep(
    private val project: Project,
    private val rootDirectory: Path,
    private val buildSystem: BuildSystem,
    private val config: ProjectConfig?,
    private val pomText: String,
    private val parts: Iterable<MavenStepFunc> = defaultParts
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        Files.createDirectories(rootDirectory)

        runWriteTask {
            if (project.isDisposed) {
                return@runWriteTask
            }
            val pomPsi = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, pomText)
                ?: return@runWriteTask

            pomPsi.name = "pom.xml"

            val pomXmlPsi = pomPsi as XmlFile
            pomPsi.runWriteAction {
                if (project.isDisposed) {
                    return@runWriteAction
                }
                val manager = DomManager.getDomManager(project)
                val mavenProjectXml = manager.getFileElement(pomXmlPsi, MavenDomProjectModel::class.java)?.rootElement
                    ?: return@runWriteAction

                val root = pomXmlPsi.rootTag ?: return@runWriteAction

                for (part in parts) {
                    MinecraftProjectCreator.WorkLogStep.currentLog?.newCurrentStep(part, 1)
                    part(this, mavenProjectXml, root)
                }
                MinecraftProjectCreator.WorkLogStep.currentLog?.finishCurrentStep()

                val vRootDir = rootDirectory.virtualFile
                    ?: throw IllegalStateException("Unable to find root directory: $rootDirectory")
                val dir = PsiManager.getInstance(project).findDirectory(vRootDir) ?: return@runWriteAction
                dir.findFile(pomPsi.name)?.delete()
                dir.add(pomPsi)

                vRootDir.refresh(false, false)
                val pomFile = vRootDir.findChild(pomPsi.name) ?: return@runWriteAction

                // Reformat the code to match their code style
                PsiManager.getInstance(project).findFile(pomFile)?.let {
                    ReformatCodeProcessor(it, false).run()
                }
            }
        }
    }

    companion object {
        val pluginVersions by lazy {
            runBlocking {
                getVersionJson<Map<String, String>>("maven.json")
            }
        }

        private val defaultParts = listOf(setupDirs(), setupCore(), setupName(), setupInfo(), setupDependencies())

        fun setupDirs(): MavenStepFunc = { step, _, _ ->
            step.buildSystem.directories = DirectorySet.create(step.rootDirectory)
        }

        fun setupCore(): MavenStepFunc = { step, model, _ ->
            model.groupId.value = step.buildSystem.groupId
            model.artifactId.value = step.buildSystem.artifactId
            model.version.value = step.buildSystem.version
        }

        fun setupSubCore(parentArtifactId: String): MavenStepFunc {
            return { step, model, _ ->
                model.mavenParent.groupId.value = step.buildSystem.groupId
                model.mavenParent.artifactId.value = parentArtifactId
                model.mavenParent.version.value = step.buildSystem.version
                model.artifactId.value = step.buildSystem.artifactId
            }
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun setupName(): MavenStepFunc = { step, model, _ ->
            model.name.value = step.config?.pluginName
        }

        fun setupSubName(type: PlatformType): MavenStepFunc = { step, model, _ ->
            model.name.value = step.config?.pluginName + " " + type.normalName
        }

        fun setupInfo(): MavenStepFunc = { step, _, root ->
            val properties = root.findFirstSubTag("properties")

            if (step.config?.hasWebsite() == true) {
                val url = root.createChildTag("url", null, step.config.website, false)
                root.addAfter(url, properties)
            }

            if (step.config?.hasDescription() == true) {
                val description = root.createChildTag("description", null, step.config.description, false)
                root.addBefore(description, properties)
            }
        }

        fun setupDependencies(): MavenStepFunc = { step, model, _ ->
            for ((id, url, types) in step.buildSystem.repositories) {
                if (!types.contains(BuildSystemType.MAVEN)) {
                    continue
                }
                val repository = model.repositories.addRepository()
                repository.id.value = id
                repository.url.value = url
            }

            for ((depGroupId, depArtifactId, depVersion, scope) in step.buildSystem.dependencies) {
                if (scope == null) {
                    continue
                }
                val dependency = model.dependencies.addDependency()
                dependency.groupId.value = depGroupId
                dependency.artifactId.value = depArtifactId
                dependency.version.value = depVersion
                dependency.scope.value = scope
            }
        }

        fun setupModules(moduleNames: Iterable<String>): MavenStepFunc = { _, model, _ ->
            for (moduleName in moduleNames) {
                val mod = model.modules.addModule()
                mod.stringValue = moduleName
            }
        }
    }
}

class BasicMavenFinalizerStep(
    private val rootModule: Module,
    private val rootDirectory: Path
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        if (rootModule.isDisposed || rootModule.project.isDisposed) {
            return
        }

        val project = rootModule.project

        val pomFile = rootDirectory.resolve("pom.xml")
        val vPomFile = pomFile.virtualFile ?: throw IllegalStateException("Could not find file: $pomFile")

        // Force Maven to setup the project
        invokeLater(project.disposed) {
            val manager = MavenProjectsManager.getInstance(project)
            manager.addManagedFilesOrUnignore(listOf(vPomFile))
            manager.importingSettings.isDownloadDocsAutomatically = true
            manager.importingSettings.isDownloadSourcesAutomatically = true

            // Setup the default Maven run config
            val params = MavenRunnerParameters()
            params.workingDirPath = rootDirectory.toAbsolutePath().toString()
            params.goals = listOf("clean", "package")
            val runnerSettings = MavenRunConfigurationType
                .createRunnerAndConfigurationSettings(null, null, params, project)
            runnerSettings.name = rootModule.name + " build"
            runnerSettings.storeInLocalWorkspace()

            val runManager = RunManager.getInstance(project)
            runManager.addConfiguration(runnerSettings)
            if (runManager.selectedConfiguration == null) {
                runManager.selectedConfiguration = runnerSettings
            }
        }
    }
}

class CommonModuleDependencyStep(private val buildSystem: BuildSystem) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        buildSystem.dependencies.add(
            BuildDependency(
                buildSystem.groupId,
                buildSystem.commonModuleName,
                "\${project.version}",
                mavenScope = "compile"
            )
        )
    }
}

class MavenGitignoreStep(
    private val project: Project,
    private val rootDirectory: Path
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val gitignoreFile = rootDirectory.resolve(".gitignore")

        val fileText = BuildSystemTemplate.applyMavenGitignore(project)

        Files.write(gitignoreFile, fileText.toByteArray(Charsets.UTF_8), CREATE, WRITE, TRUNCATE_EXISTING)
    }
}
