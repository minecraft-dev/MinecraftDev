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
import com.demonwav.mcdev.platform.sponge.SpongeTemplate
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.localFile
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.RunManager
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
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

class GradleBuildSystem : BuildSystem() {

    var buildGradle: VirtualFile? = null

    override fun create(project: Project, configuration: ProjectConfiguration, indicator: ProgressIndicator) {
        rootDirectory.refresh(false,  true)
        createDirectories()

        if (configuration.type == PlatformType.FORGE || configuration is SpongeForgeProjectConfiguration) {
            handleForgeCreate(project, configuration, indicator)
        } else if (configuration.type == PlatformType.LITELOADER) {
            handleLiteLoaderCreate(project, configuration, indicator)
        } else {
            handleGeneralCreate(project, configuration, indicator)
        }

        // The file needs to be saved, if not Gradle will see the file without the dependencies and won't import correctly
        if (buildGradle == null) {
            return
        }

        saveFile(buildGradle)
    }

    private fun handleForgeCreate(project: Project, configuration: ProjectConfiguration, indicator: ProgressIndicator) {
        if (configuration !is ForgeProjectConfiguration) {
            return
        }

        runWriteTask {
            val gradleProp = setupGradleFiles()

            ForgeTemplate.applyBuildGradleTemplate(project, buildGradle!!, gradleProp, groupId, artifactId, configuration, version)

            if (configuration is SpongeForgeProjectConfiguration) {
                val buildGradlePsi = PsiManager.getInstance(project).findFile(buildGradle!!)
                buildGradlePsi?.let { addBuildGradleDependencies(project, it, false) }
            }
        }

        setupWrapper(project, indicator)
        setupDecompWorkspace(project, indicator)
    }

    private fun handleLiteLoaderCreate(project: Project, configuration: ProjectConfiguration, indicator: ProgressIndicator) {
        if (configuration !is LiteLoaderProjectConfiguration) {
            return
        }

        runWriteTask {
            val gradleProp = setupGradleFiles()
            LiteLoaderTemplate.applyBuildGradleTemplate(project, buildGradle!!, gradleProp, groupId, artifactId, configuration)
        }

        setupWrapper(project, indicator)
        setupDecompWorkspace(project, indicator)
    }

    private fun handleGeneralCreate(project: Project, configuration: ProjectConfiguration, indicator: ProgressIndicator) {
        runWriteTask {
            val gradleProp = rootDirectory.findOrCreateChildData(this, "gradle.properties")

            val buildGradleText = if (configuration.type == PlatformType.SPONGE) {
                SpongeTemplate.applyBuildGradleTemplate(project, gradleProp, groupId, artifactId, version, buildVersion)
            } else {
                BaseTemplate.applyBuildGradleTemplate(project, gradleProp, groupId, artifactId, version, buildVersion)
            } ?: return@runWriteTask

            addBuildGradleDependencies(project, buildGradleText)
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
        }

        setupWrapper(project, indicator)
    }

    private fun setupGradleFiles(): VirtualFile {
        buildGradle = rootDirectory.findOrCreateChildData(this, "build.gradle")
        return rootDirectory.findOrCreateChildData(this, "gradle.properties")
    }

    private fun setupWrapper(project: Project, indicator: ProgressIndicator) {
        // Setup gradle wrapper
        // We'll write the properties file to ensure it sets up with the right version
        runWriteTask {
            val wrapperDirPath = VfsUtil.createDirectoryIfMissing(rootDirectory, "gradle/wrapper").path
            FileUtils.writeLines(File(wrapperDirPath, "gradle-wrapper.properties"), listOf(
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-4.4.1-bin.zip"
            ))
        }

        runGradleTask(project, indicator) { launcher ->
            launcher.forTasks("wrapper")
        }
    }

    private fun setupDecompWorkspace(project: Project, indicator: ProgressIndicator) {
        runGradleTask(project, indicator) { launcher ->
            launcher.forTasks("setupDecompWorkspace").setJvmArguments("-Xmx2G")
        }
    }

    private inline fun runGradleTask(project: Project, indicator: ProgressIndicator, func: (BuildLauncher) -> Unit) {
        val connector = GradleConnector.newConnector()
        connector.forProjectDirectory(rootDirectory.localFile)
        val connection = connector.connect()
        val launcher = connection.newBuild()

        try {
            val sdkPair = ExternalSystemJdkUtil.getAvailableJdk(project)
            if (sdkPair.second?.homePath != null && ExternalSystemJdkUtil.USE_INTERNAL_JAVA != sdkPair.first) {
                launcher.setJavaHome(File(sdkPair.second.homePath))
            }

            launcher.addProgressListener(ProgressListener {
                indicator.text = it.description
            })
            func(launcher)
            launcher.run()
        } finally {
            connection.close()
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
                it.children.any { it is GrReferenceExpression && it.text == name }
            }.map {
                // We want to find the grandchild which is a GrClosable block, this is the
                // basis for the method block
                it.children.mapNotNull { it as? GrClosableBlock }.firstOrNull()
            }.filterNotNull()
            .firstOrNull()

    private fun addBuildGradleDependencies(project: Project, file: PsiFile, addToDirectory: Boolean) {
        // Write the repository and dependency data to the psi file
        file.runWriteAction {
            val newBuildGradle = rootDirectory.findOrCreateChildData(this, "build.gradle")

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
                    newBuildGradle.delete(this)

                    rootDirectoryPsi.add(file)
                }
            }

            buildGradle = rootDirectory.findChild("build.gradle") ?: return@runWriteAction

            // Reformat code to match their code style
            val newBuildGradlePsi = PsiManager.getInstance(project).findFile(buildGradle!!) ?: return@runWriteAction
            ReformatCodeProcessor(newBuildGradlePsi, false).run()
        }
    }

    private fun addBuildGradleDependencies(project: Project, text: String) {
        // Create the Psi file from the text, but don't write it until we are finished with it
        val buildGradlePsi = PsiFileFactory.getInstance(project).createFileFromText(GroovyLanguage, text)

        addBuildGradleDependencies(project, buildGradlePsi, true)
    }

    private fun saveFile(file: VirtualFile?) {
        file ?: return

        runWriteTask {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runWriteTask
            FileDocumentManager.getInstance().saveDocument(document)
        }
    }

    override fun finishSetup(rootModule: Module, configurations: Collection<ProjectConfiguration>, indicator: ProgressIndicator) {
        val project = rootModule.project

        // Tell Gradle to import this project
        val projectDataManager = ServiceManager.getService(ProjectDataManager::class.java)
        val gradleProjectImportBuilder = GradleProjectImportBuilder(projectDataManager)
        val gradleProjectImportProvider = GradleProjectImportProvider(gradleProjectImportBuilder)

        // Shadow name for ease of use, make non null
        val buildGradle = buildGradle ?: return

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

            val settings = RunnerAndConfigurationSettingsImpl(
                RunManagerImpl.getInstanceImpl(project),
                runConfiguration,
                false
            )

            settings.isActivateToolWindowBeforeRun = true
            settings.isSingleton = true

            val runManager = RunManager.getInstance(project)
            runManager.addConfiguration(settings, false)
            if (runManager.selectedConfiguration == null) {
                runManager.selectedConfiguration = settings
            }
        }
    }

    private fun requestCreateForgeRunConfigs(project: Project, rootModule: Module, configurations: Collection<ProjectConfiguration>) {
        runWriteTaskLater {
            // Basically mark this as a newly created project
            val gradleDir = VfsUtil.createDirectoryIfMissing(project.baseDir, ".gradle")
            val hello = gradleDir.findOrCreateChildData(this, HELLO)

            hello.setBinaryContent((rootModule.name + "\n" + configurations.size).toByteArray(Charsets.UTF_8))
        }
    }

    fun createMultiModuleProject(project: Project,
                                 configurations: Map<PlatformType, ProjectConfiguration>,
                                 indicator: ProgressIndicator): Map<GradleBuildSystem, ProjectConfiguration> {

        val map = mutableMapOf<GradleBuildSystem, ProjectConfiguration>()

        setupWrapper(project, indicator)

        rootDirectory.refresh(false, true)

        // Create the includes string for settings.gradle
        val includes = "'${pluginName.toLowerCase()}-common', " +
            configurations.values.joinToString(", ") { "'${pluginName.toLowerCase()}-${it.type.name.toLowerCase()}'" }

        val artifactIdLower = artifactId.toLowerCase()

        runWriteTask {
            // Write the parent files to disk so the children modules can import correctly
            val gradleProp = setupGradleFiles()
            val settingsGradle = rootDirectory.createChildData(this, "settings.gradle")

            BaseTemplate.applyMultiModuleBuildGradleTemplate(
                project, buildGradle!!, gradleProp, groupId, artifactId, version, buildVersion, configurations
            )

            BaseTemplate.applySettingsGradleTemplate(project, settingsGradle, artifactIdLower, includes)

            // Common will be empty, it's for the developers to fill in with common classes
            val common = rootDirectory.createChildDirectory(this, artifactIdLower + "-common")
            createDirectories(common)
        }

        for (configuration in configurations.values) {
            // We associate each configuration with the given build system, which we add to the map at the end
            val gradleBuildSystem = GradleBuildSystem()

            runWriteTask {
                gradleBuildSystem.rootDirectory =
                    rootDirectory.createChildDirectory(this, artifactIdLower + "-" + configuration.type.name.toLowerCase())
            }

            gradleBuildSystem.artifactId = artifactId
            gradleBuildSystem.groupId = groupId
            gradleBuildSystem.version = version

            gradleBuildSystem.pluginName = pluginName
            gradleBuildSystem.buildVersion = buildVersion

            // it knows which dependencies are needed for each configuration
            MinecraftProjectCreator.addDependencies(configuration, gradleBuildSystem)

            // For each build system we initialize it, but not the same as a normal create. We need to know the common project name,
            // as we automatically add it as a dependency too
            gradleBuildSystem.createSubModule(project, configuration, artifactIdLower + "-common", indicator)
            map.putIfAbsent(gradleBuildSystem, configuration)
        }

        return map
    }

    private fun createSubModule(project: Project, configuration: ProjectConfiguration, commonProjectName: String, indicator: ProgressIndicator) {
        rootDirectory.refresh(false, true)
        createDirectories()

        // This is mostly the same as a normal create, but we use different files and don't setup the wrapper
        if (configuration.type == PlatformType.FORGE || configuration is SpongeForgeProjectConfiguration) {
            handleForgeSubCreate(project, configuration, commonProjectName, indicator)
        } else if (configuration.type == PlatformType.LITELOADER) {
            handleLiteLoaderSubCreate(project, configuration, commonProjectName, indicator)
        } else {
            handleGeneralSubCreate(project, configuration, commonProjectName)
        }

        // The file needs to be saved, if not Gradle will see the file without the dependencies and won't import correctly
        if (buildGradle == null) {
            return
        }

        saveFile(buildGradle)
    }

    private fun handleForgeSubCreate(project: Project, configuration: ProjectConfiguration, commonProjectName: String, indicator: ProgressIndicator) {
        if (configuration !is ForgeProjectConfiguration) {
            return
        }

        runWriteTask {
            val gradleProp = setupGradleFiles()

            ForgeTemplate.applySubmoduleBuildGradleTemplate(project, buildGradle!!, gradleProp, artifactId, configuration, commonProjectName)

            // We're only going to write the dependencies if it's a sponge forge project
            if (configuration is SpongeForgeProjectConfiguration) {
                val buildGradlePsi = PsiManager.getInstance(project).findFile(buildGradle!!)
                if (buildGradlePsi != null) {
                    addBuildGradleDependencies(project, buildGradlePsi, false)
                }
            }
        }

        setupDecompWorkspace(project, indicator)
    }

    private fun handleLiteLoaderSubCreate(project: Project, configuration: ProjectConfiguration, commonProjectName: String, indicator: ProgressIndicator) {
        if (configuration !is LiteLoaderProjectConfiguration) {
            return
        }

        runWriteTask {
            val gradleProp = setupGradleFiles()

            LiteLoaderTemplate.applySubmoduleBuildGradleTemplate(project, buildGradle!!, gradleProp, configuration, commonProjectName)
        }

        setupDecompWorkspace(project, indicator)
    }

    private fun handleGeneralSubCreate(project: Project, configuration: ProjectConfiguration, commonProjectName: String) {
        runWriteTask {
            val buildGradleText = if (configuration.type == PlatformType.SPONGE) {
                SpongeTemplate.applySubmoduleBuildGradleTemplate(project, commonProjectName)
            } else {
                BaseTemplate.applySubmoduleBuildGradleTemplate(project, commonProjectName)
            } ?: return@runWriteTask

            addBuildGradleDependencies(project, buildGradleText)
        }
    }

    companion object {
        const val HELLO = ".hello_from_mcdev"
    }
}
