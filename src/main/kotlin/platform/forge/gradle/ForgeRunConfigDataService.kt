/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle

import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.forge.creator.ForgeRunConfigsStep
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.localFile
import com.demonwav.mcdev.util.runGradleTaskAndWait
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import org.jetbrains.plugins.gradle.util.GradleConstants

class ForgeRunConfigDataService : AbstractProjectDataService<ProjectData, Project>() {

    override fun getTargetDataKey() = ProjectKeys.PROJECT

    override fun postProcess(
        toImport: Collection<DataNode<ProjectData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        if (projectData == null || projectData.owner != GradleConstants.SYSTEM_ID) {
            return
        }

        val baseDir = project.guessProjectDir() ?: return
        val baseDirPath = baseDir.localFile.toPath()
        val hello = baseDirPath.resolve(Paths.get(".gradle", ForgeRunConfigsStep.HELLO))
        if (!Files.isRegularFile(hello)) {
            return
        }

        val lines = Files.readAllLines(hello, Charsets.UTF_8)
        if (lines.size < 4) {
            return
        }

        val (moduleName, mcVersion, forgeVersion, task) = lines
        val mcVersionParsed = SemanticVersion.parse(mcVersion)
        val forgeVersionParsed = SemanticVersion.parse(forgeVersion)

        val moduleMap = modelsProvider.modules.associateBy { it.name }
        val module = moduleMap[moduleName] ?: return

        // We've found the module we were expecting, so we can assume the project imported correctly
        Files.delete(hello)

        val isPre113 = mcVersionParsed < ForgeModuleType.FG3_MC_VERSION
        if (isPre113 && forgeVersionParsed < ForgeModuleType.FG3_FORGE_VERSION) {
            manualCreate(project, moduleMap, module)
        } else {
            genIntellijRuns(project, moduleMap, module, task, hasData = !isPre113)
        }
    }

    private fun manualCreate(
        project: Project,
        moduleMap: Map<String, Module>,
        module: Module
    ) {
        invokeLater {
            val mainModule = findMainModule(moduleMap, module)

            val runManager = RunManager.getInstance(project)
            val factory = ApplicationConfigurationType.getInstance().configurationFactories.first()

            // Client run config
            val clientSettings = runManager.createConfiguration(module.name + " run client", factory)
            val runClientConfig = clientSettings.configuration as ApplicationConfiguration
            runClientConfig.isAllowRunningInParallel = false

            val runningDir = File(project.basePath, "run")
            if (!runningDir.exists()) {
                runningDir.mkdir()
            }

            runClientConfig.workingDirectory = project.basePath + File.separator + "run"
            runClientConfig.mainClassName = "GradleStart"
            runClientConfig.setModule(mainModule)

            clientSettings.isActivateToolWindowBeforeRun = true

            clientSettings.storeInLocalWorkspace()
            runManager.addConfiguration(clientSettings)
            runManager.selectedConfiguration = clientSettings

            // Server run config
            val serverSettings = runManager.createConfiguration(module.name + " run server", factory)
            val runServerConfig = serverSettings.configuration as ApplicationConfiguration
            runServerConfig.isAllowRunningInParallel = false

            runServerConfig.mainClassName = "GradleStartServer"
            runServerConfig.programParameters = "nogui"
            runServerConfig.workingDirectory = project.basePath + File.separator + "run"
            runServerConfig.setModule(mainModule)

            serverSettings.isActivateToolWindowBeforeRun = true
            serverSettings.storeInLocalWorkspace()
            runManager.addConfiguration(serverSettings)
        }
    }

    private fun genIntellijRuns(
        project: Project,
        moduleMap: Map<String, Module>,
        module: Module,
        task: String,
        hasData: Boolean
    ) {
        val mainModule = findMainModule(moduleMap, module)

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "genIntellijRuns", false) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true

                    val projectDir = project.guessProjectDir() ?: return
                    indicator.text = "Creating run configurations"
                    indicator.text2 = "Running Gradle task: '$task'"
                    runGradleTaskAndWait(project, projectDir.localFile.toPath()) { settings ->
                        settings.taskNames = listOf(task)
                    }

                    cleanupGeneratedRuns(project, mainModule, hasData)
                }
            }
        )
    }

    private fun cleanupGeneratedRuns(project: Project, module: Module, hasData: Boolean) {
        invokeAndWait {
            if (!module.isDisposed) {
                ForgeRunManagerListener(module, hasData)
            }
        }

        project.guessProjectDir()?.let { dir ->
            LocalFileSystem.getInstance().refreshFiles(listOf(dir), true, true, null)
        }
    }

    private fun findMainModule(moduleMap: Map<String, Module>, module: Module): Module {
        return moduleMap[module.name + ".main"] ?: module
    }
}

class ForgeRunManagerListener(private val module: Module, hasData: Boolean) : RunManagerListener {
    private val count = AtomicInteger(3)
    private val disposable = Disposer.newDisposable()

    init {
        Disposer.register(module, disposable)
        module.project.messageBus.connect(disposable).subscribe(RunManagerListener.TOPIC, this)
        // If we don't have a data run, don't wait for it
        if (!hasData) {
            count.decrementAndGet()
        }
    }

    override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
        val config = settings.configuration as? ApplicationConfiguration ?: return

        val postFixes = arrayOf("runClient", "runServer", "runData")
        if (postFixes.none { settings.name.endsWith(it) }) {
            return
        }

        config.isAllowRunningInParallel = false
        config.setModule(module)
        RunManager.getInstance(module.project).addConfiguration(settings)

        if (count.decrementAndGet() == 0) {
            Disposer.dispose(disposable)
        }
    }
}
