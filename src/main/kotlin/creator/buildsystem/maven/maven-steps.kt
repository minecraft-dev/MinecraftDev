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

import com.demonwav.mcdev.creator.*
import com.demonwav.mcdev.creator.buildsystem.*
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.*
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.RunManager
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.project.importing.MavenImportingManager

private val pluginVersions by lazy {
    runBlocking {
        getVersionJson<Map<String, String>>("maven.json")
    }
}

fun FixedAssetsNewProjectWizardStep.addDefaultMavenProperties() {
    addTemplateProperties(pluginVersions)
}

abstract class AbstractPatchPomStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description = "Patching pom.xml"

    open fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        setupCore(model)
        setupName(model)
        setupInfo(root)
    }

    protected fun setupCore(model: MavenDomProjectModel) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        model.groupId.value = buildSystemProps.groupId
        model.artifactId.value = buildSystemProps.artifactId
        model.version.value = buildSystemProps.version
    }

    protected fun setupName(model: MavenDomProjectModel) {
        val name = data.getUserData(AbstractModNameStep.KEY) ?: return
        model.name.value = name
    }

    protected fun setupInfo(root: XmlTag) {
        val website = data.getUserData(WebsiteStep.KEY)
        val description = data.getUserData(DescriptionStep.KEY)

        val properties = root.findFirstSubTag("properties")
        if (!website.isNullOrBlank()) {
            val url = root.createChildTag("url", null, website, false)
            root.addAfter(url, properties)
        }

        if (!description.isNullOrBlank()) {
            val descriptionTag = root.createChildTag("description", null, description, false)
            root.addBefore(descriptionTag, properties)
        }
    }

    protected fun setupDependencies(model: MavenDomProjectModel, repositories: List<BuildRepository>, dependencies: List<BuildDependency>) {
        for ((id, url, types) in repositories) {
            if (!types.contains(BuildSystemType.MAVEN)) {
                continue
            }
            val repository = model.repositories.addRepository()
            repository.id.value = id
            repository.url.value = url
        }

        for ((depGroupId, depArtifactId, depVersion, scope) in dependencies) {
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

    override fun perform(project: Project) {
        invokeAndWait {
            if (project.isDisposed) {
                return@invokeAndWait
            }

            runWriteTask {
                val pomFile = VfsUtil.findFile(Path.of(context.projectFileDirectory, "pom.xml"), true)
                    ?: return@runWriteTask
                val pomPsi = PsiManager.getInstance(project).findFile(pomFile) as? XmlFile ?: return@runWriteTask

                pomPsi.name = "pom.xml"

                NonProjectFileWritingAccessProvider.disableChecksDuring {
                    pomPsi.runWriteAction {
                        val manager = DomManager.getDomManager(project)
                        val mavenProjectXml =
                            manager.getFileElement(pomPsi, MavenDomProjectModel::class.java)?.rootElement
                                ?: return@runWriteAction

                        val root = pomPsi.rootTag ?: return@runWriteAction

                        patchPom(mavenProjectXml, root)

                        // The maven importer requires that the document is saved to disk
                        val document = PsiDocumentManager.getInstance(project).getDocument(pomPsi)
                            ?: return@runWriteAction
                        FileDocumentManager.getInstance().saveDocument(document)
                    }
                }
            }
        }
    }
}

class ReformatPomStep(parent: NewProjectWizardStep) : AbstractReformatFilesStep(parent) {
    override fun addFilesToReformat() {
        addFileToReformat("pom.xml")
    }
}

class MavenImportStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description = "Importing Maven project"

    override fun perform(project: Project) {
        val pomFile = VfsUtil.findFile(Path.of(context.projectFileDirectory).resolve("pom.xml"), true)
            ?: return
        val promise = invokeAndWait {
            if (project.isDisposed) {
                return@invokeAndWait null
            }
            MavenImportingManager.getInstance(project).linkAndImportFile(pomFile)
        } ?: return

        promise.blockingGet(Int.MAX_VALUE, TimeUnit.SECONDS)
    }
}

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
