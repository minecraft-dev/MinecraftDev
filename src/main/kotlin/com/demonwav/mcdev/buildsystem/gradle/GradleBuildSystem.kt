/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.gradle

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.forge.ForgeTemplate
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration
import com.demonwav.mcdev.platform.liteloader.LiteLoaderProjectConfiguration
import com.demonwav.mcdev.platform.liteloader.LiteLoaderTemplate
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
import com.demonwav.mcdev.platform.sponge.SpongeTemplate
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.localFile
import com.demonwav.mcdev.util.refreshFs
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.RunManager
import com.intellij.ide.actions.ImportModuleAction
import com.intellij.ide.util.newProjectWizard.AddModuleWizard
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.apache.commons.io.FileUtils
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProgressListener
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.GroovyLanguage
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import java.io.File

class GradleBuildSystem(
    artifactId: String,
    groupId: String,
    version: String
) : BuildSystem(artifactId, groupId, version) {

    data class ProjectDescriptor(
        val rootDirectory: VirtualFile,
        val project: Project
    )

    override fun create(
        project: Project,
        rootDirectory: VirtualFile,
        configuration: ProjectConfiguration,
        indicator: ProgressIndicator,
        pluginName: String
    ) {
        if (project.isDisposed) {
            return
        }
        rootDirectory.refresh(false, true)
        directories = createDirectories(rootDirectory)

        val descriptor = ProjectDescriptor(rootDirectory, project)

        when (configuration) {
            is ForgeProjectConfiguration -> handleForgeCreate(descriptor, configuration, indicator)
            is LiteLoaderProjectConfiguration -> handleLiteLoaderCreate(descriptor, configuration, indicator)
            else -> handleGeneralCreate(descriptor, configuration, indicator)
        }

        val buildGradle = rootDirectory.findChild("build.gradle") ?: return
        saveFile(buildGradle)
    }

    private fun handleForgeCreate(descriptor: ProjectDescriptor, configuration: ForgeProjectConfiguration, indicator: ProgressIndicator) {
        val (rootDirectory, project) = descriptor
        runWriteTask {
            val (buildGradle, gradleProp) = setupGradleFiles(rootDirectory)

            ForgeTemplate.applyBuildGradleTemplate(
                project, buildGradle, gradleProp, groupId, artifactId, configuration, version
            )
            val newBuildGradle = buildGradle.refreshFs()

            if (configuration is SpongeForgeProjectConfiguration) {
                val buildGradlePsi = PsiManager.getInstance(project).findFile(newBuildGradle)
                buildGradlePsi?.let { addBuildGradleDependencies(descriptor, it, false) }
            }
        }

        setupWrapper(descriptor, indicator)
        setupDecompWorkspace(descriptor, indicator)
    }

    private fun handleLiteLoaderCreate(descriptor: ProjectDescriptor, configuration: LiteLoaderProjectConfiguration, indicator: ProgressIndicator) {
        runWriteTask {
            val (buildGradle, gradleProp) = setupGradleFiles(descriptor.rootDirectory)

            LiteLoaderTemplate.applyBuildGradleTemplate(
                descriptor.project, buildGradle, gradleProp, groupId, artifactId, configuration
            )
        }

        setupWrapper(descriptor, indicator)
        setupDecompWorkspace(descriptor, indicator)
    }

    private fun handleGeneralCreate(descriptor: ProjectDescriptor, configuration: ProjectConfiguration, indicator: ProgressIndicator) {
        val (rootDirectory, project) = descriptor
        runWriteTask {
            val (_, gradleProp) = setupGradleFiles(rootDirectory)

            val buildGradleText = if (configuration is SpongeProjectConfiguration) {
                SpongeTemplate.applyBuildGradleTemplate(project, gradleProp, groupId, artifactId, version)
            } else {
                BaseTemplate.applyBuildGradleTemplate(project, gradleProp, groupId, artifactId, version)
            } ?: return@runWriteTask

            addBuildGradleDependencies(descriptor, buildGradleText)
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
        }

        setupWrapper(descriptor, indicator)
    }

    data class GradleFiles(val buildGradle: VirtualFile, val gradleProperties: VirtualFile)

    private fun setupGradleFiles(dir: VirtualFile): GradleFiles {
        return GradleFiles(
            dir.findOrCreateChildData(this, "build.gradle"),
            dir.findOrCreateChildData(this, "gradle.properties")
        )
    }

    private fun setupWrapper(descriptor: ProjectDescriptor, indicator: ProgressIndicator) {
        // Setup gradle wrapper
        // We'll write the properties file to ensure it sets up with the right version
        runWriteTask {
            val wrapperDirPath = VfsUtil.createDirectoryIfMissing(descriptor.rootDirectory, "gradle/wrapper").path
            FileUtils.writeLines(File(wrapperDirPath, "gradle-wrapper.properties"), listOf(
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-4.4.1-bin.zip"
            ))
        }

        runGradleTask(descriptor, indicator) { launcher ->
            launcher.forTasks("wrapper")
        }
    }

    private fun setupDecompWorkspace(descriptor: ProjectDescriptor, indicator: ProgressIndicator) {
        runGradleTask(descriptor, indicator) { launcher ->
            launcher.forTasks("setupDecompWorkspace").setJvmArguments("-Xmx2G")
        }
    }

    private inline fun runGradleTask(
        descriptor: ProjectDescriptor,
        indicator: ProgressIndicator,
        func: (BuildLauncher) -> Unit
    ) {
        val (rootDirectory, project) = descriptor

        val connector = GradleConnector.newConnector()
        connector.forProjectDirectory(rootDirectory.localFile)
        val connection = connector.connect()
        val launcher = connection.newBuild()

        connection.use {
            val sdkPair = ExternalSystemJdkUtil.getAvailableJdk(project)
            if (sdkPair.second?.homePath != null && ExternalSystemJdkUtil.USE_INTERNAL_JAVA != sdkPair.first) {
                launcher.setJavaHome(File(sdkPair.second.homePath))
            }

            launcher.addProgressListener(ProgressListener { event ->
                indicator.text = event.description
            })
            func(launcher)
            launcher.run()
        }
    }

    private fun createRepositoriesOrDependencies(project: Project, file: GroovyFile, name: String, expressions: List<String>) {
        // Get the block so we can start working with it
        val block = getClosableBlockByName(file, name) ?: return

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

    private fun addBuildGradleDependencies(
        descriptor: ProjectDescriptor,
        file: PsiFile,
        addToDirectory: Boolean
    ) {
        val (rootDirectory, project) = descriptor
        // Write the repository and dependency data to the psi file
        file.runWriteAction {
            var buildGradle = rootDirectory.findOrCreateChildData(this, "build.gradle")

            file.name = "build.gradle"

            val groovyFile = file as GroovyFile

            createRepositoriesOrDependencies(
                project,
                groovyFile,
                "repositories",
                repositories.map { "maven {name = '${it.id}'\nurl = '${it.url}'\n}" }
            )

            createRepositoriesOrDependencies(
                project,
                groovyFile,
                "dependencies",
                dependencies.map { "compile '${it.groupId}:${it.artifactId}:${it.version}'" }
            )

            ReformatCodeProcessor(file, false).run()
            if (addToDirectory) {
                val rootDirectoryPsi = PsiManager.getInstance(project).findDirectory(rootDirectory)
                if (rootDirectoryPsi != null) {
                    buildGradle.delete(this)

                    rootDirectoryPsi.add(file)
                }
            }

            buildGradle = buildGradle.refreshFs()

            // Reformat code to match their code style
            val newBuildGradlePsi = PsiManager.getInstance(project).findFile(buildGradle) ?: return@runWriteAction
            ReformatCodeProcessor(newBuildGradlePsi, false).run()
        }
    }

    private fun addBuildGradleDependencies(descriptor: ProjectDescriptor, text: String) {
        // Create the Psi file from the text, but don't write it until we are finished with it
        val buildGradlePsi = PsiFileFactory.getInstance(descriptor.project).createFileFromText(GroovyLanguage, text)

        addBuildGradleDependencies(descriptor, buildGradlePsi, true)
    }

    private fun saveFile(file: VirtualFile) {
        runWriteTask {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runWriteTask
            FileDocumentManager.getInstance().saveDocument(document)
        }
    }

    override fun finishSetup(
        rootModule: Module,
        rootDirectory: VirtualFile,
        configurations: Collection<ProjectConfiguration>,
        indicator: ProgressIndicator
    ) {
        val project = rootModule.project
        if (rootModule.isDisposed || project.isDisposed) {
            return
        }

        // Tell Gradle to import this project
        val projectDataManager = ServiceManager.getService(ProjectDataManager::class.java)
        val gradleProjectImportBuilder = GradleProjectImportBuilder(projectDataManager)
        val gradleProjectImportProvider = GradleProjectImportProvider(gradleProjectImportBuilder)

        val buildGradle = rootDirectory.findChild("build.gradle") ?: return

        invokeLater {
            val wizard = AddModuleWizard(project, buildGradle.path, gradleProjectImportProvider)
            if (wizard.showAndGet()) {
                ImportModuleAction.createFromWizard(project, wizard)
            }

            // Set up the run config
            // Get the gradle external task type, this is what sets it as a gradle task
            val gradleType = GradleExternalTaskConfigurationType.getInstance()

            if (configurations.any { it.type == PlatformType.FORGE || it is SpongeForgeProjectConfiguration }) {
                requestCreateForgeRunConfigs(project, rootModule, configurations)
            }

            val runManager = RunManager.getInstance(project)

            val runConfiguration = ExternalSystemRunConfiguration(
                GradleConstants.SYSTEM_ID,
                project,
                gradleType.configurationFactories[0],
                rootModule.name + " build"
            )

            // Set relevant gradle values
            runConfiguration.settings.externalProjectPath = rootDirectory.path
            runConfiguration.settings.executionName = rootModule.name + " build"
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

    private fun requestCreateForgeRunConfigs(project: Project, rootModule: Module, configurations: Collection<ProjectConfiguration>) {
        runWriteTaskLater {
            // Basically mark this as a newly created project
            val basePath = project.basePath ?: return@runWriteTaskLater // If this is null there's not much we can do
            val gradleDir = VfsUtil.createDirectoryIfMissing("$basePath/.gradle")
            val hello = gradleDir?.findOrCreateChildData(this, HELLO) ?: return@runWriteTaskLater

            hello.setBinaryContent((rootModule.name + "\n" + configurations.size).toByteArray(Charsets.UTF_8))
        }
    }

    fun createMultiModuleProject(
        rootDirectory: VirtualFile,
        project: Project,
        configurations: LinkedHashSet<ProjectConfiguration>,
        indicator: ProgressIndicator,
        pluginName: String
    ): Map<GradleBuildSystem, ProjectConfiguration> {
        val map = mutableMapOf<GradleBuildSystem, ProjectConfiguration>()

        setupWrapper(ProjectDescriptor(rootDirectory, project), indicator)

        rootDirectory.refresh(false, true)

        // Create the includes string for settings.gradle
        val includes = "'${pluginName.toLowerCase()}-common', " +
            configurations.joinToString(", ") { "'${pluginName.toLowerCase()}-${it.type.name.toLowerCase()}'" }

        val artifactIdLower = artifactId.toLowerCase()

        runWriteTask {
            // Write the parent files to disk so the children modules can import correctly
            val (buildGradle, gradleProp) = setupGradleFiles(rootDirectory)
            val settingsGradle = rootDirectory.createChildData(this, "settings.gradle")

            BaseTemplate.applyMultiModuleBuildGradleTemplate(
                project, buildGradle, gradleProp, groupId, artifactId, version, configurations
            )

            BaseTemplate.applySettingsGradleTemplate(project, settingsGradle, artifactIdLower, includes)

            // Common will be empty, it's for the developers to fill in with common classes
            val common = rootDirectory.createChildDirectory(this, "$artifactIdLower-common")
            createDirectories(common)
        }

        for (configuration in configurations) {
            // We associate each configuration with the given build system, which we add to the map at the end
            val gradleBuildSystem = GradleBuildSystem(artifactId, groupId, version)

            // it knows which dependencies are needed for each configuration
            MinecraftProjectCreator.addDependencies(configuration, gradleBuildSystem)

            val newRootDir = runWriteTask {
                rootDirectory.createChildDirectory(this, artifactIdLower + "-" + configuration.type.name.toLowerCase())
            }

            // For each build system we initialize it, but not the same as a normal create. We need to know the common project name,
            // as we automatically add it as a dependency too
            val newDesc = ProjectDescriptor(newRootDir, project)
            gradleBuildSystem.createSubModule(newDesc, configuration, "$artifactIdLower-common", indicator)
            map.putIfAbsent(gradleBuildSystem, configuration)
        }

        return map
    }

    private fun createSubModule(
        descriptor: ProjectDescriptor,
        configuration: ProjectConfiguration,
        commonProjectName: String,
        indicator: ProgressIndicator
    ) {
        descriptor.rootDirectory.let { dir ->
            dir.refresh(false, true)
            directories = createDirectories(dir)
        }

        // This is mostly the same as a normal create, but we use different files and don't setup the wrapper
        if (configuration.type == PlatformType.FORGE || configuration is SpongeForgeProjectConfiguration) {
            handleForgeSubCreate(descriptor, configuration, commonProjectName, indicator)
        } else if (configuration.type == PlatformType.LITELOADER) {
            handleLiteLoaderSubCreate(descriptor, configuration, commonProjectName, indicator)
        } else {
            handleGeneralSubCreate(descriptor, configuration, commonProjectName)
        }

        val buildGradle = descriptor.rootDirectory.findChild("build.gradle") ?: return
        saveFile(buildGradle)
    }

    private fun handleForgeSubCreate(
        descriptor: ProjectDescriptor,
        configuration: ProjectConfiguration,
        commonProjectName: String,
        indicator: ProgressIndicator
    ) {
        val (rootDirectory, project) = descriptor

        if (configuration !is ForgeProjectConfiguration) {
            return
        }

        runWriteTask {
            val (buildGradle, gradleProp) = setupGradleFiles(rootDirectory)

            ForgeTemplate.applySubmoduleBuildGradleTemplate(project, buildGradle, gradleProp, artifactId, configuration, commonProjectName)

            // We're only going to write the dependencies if it's a sponge forge project
            if (configuration is SpongeForgeProjectConfiguration) {
                val buildGradlePsi = PsiManager.getInstance(project).findFile(buildGradle)
                if (buildGradlePsi != null) {
                    addBuildGradleDependencies(descriptor, buildGradlePsi, false)
                }
            }
        }

        setupDecompWorkspace(descriptor, indicator)
    }

    private fun handleLiteLoaderSubCreate(
        descriptor: ProjectDescriptor,
        configuration: ProjectConfiguration,
        commonProjectName: String,
        indicator: ProgressIndicator
    ) {
        if (configuration !is LiteLoaderProjectConfiguration) {
            return
        }

        val (rootDirectory, project) = descriptor
        runWriteTask {
            val (buildGradle, gradleProp) = setupGradleFiles(rootDirectory)

            LiteLoaderTemplate.applySubmoduleBuildGradleTemplate(project, buildGradle, gradleProp, configuration, commonProjectName)
        }

        setupDecompWorkspace(descriptor, indicator)
    }

    private fun handleGeneralSubCreate(
        descriptor: ProjectDescriptor,
        configuration: ProjectConfiguration,
        commonProjectName: String
    ) {
        val (_, project) = descriptor
        runWriteTask {
            val buildGradleText = if (configuration.type == PlatformType.SPONGE) {
                SpongeTemplate.applySubmoduleBuildGradleTemplate(project, commonProjectName)
            } else {
                BaseTemplate.applySubmoduleBuildGradleTemplate(project, commonProjectName)
            } ?: return@runWriteTask

            addBuildGradleDependencies(descriptor, buildGradleText)
        }
    }

    companion object {
        const val HELLO = ".hello_from_mcdev"
    }
}
