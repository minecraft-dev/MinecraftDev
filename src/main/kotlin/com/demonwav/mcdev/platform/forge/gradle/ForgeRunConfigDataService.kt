/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle

import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.buildsystem.gradle.runGradleTask
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.localFile
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.application.subscribe
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.openapi.application.runReadAction
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
import com.intellij.openapi.vfs.VfsUtil
import java.io.File
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

        val basePath = project.basePath ?: return
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath)
        val hello = VfsUtil.findRelativeFile(baseDir, ".gradle", GradleBuildSystem.HELLO) ?: return
        if (!hello.exists()) {
            return
        }

        val (moduleName, sizeText, forgeVersion, task) = try {
            runReadAction {
                hello.inputStream.bufferedReader().use { it.readText() }.split("\n")
            }
        } finally {
            // Request this file be deleted
            // We only want to do this action one time
            runWriteTaskLater {
                hello.delete(this)
            }
        }

        val size = sizeText.toIntOrNull() ?: return

        val moduleMap = modelsProvider.modules.associateBy { it.name }

        // Different versions of IntelliJ seem to have different ways of naming modules...this is probably unnecessary
        // but I don't really know the correct way to do this. Also not totally confident this will always work
        val rootModule = moduleMap[moduleName]
            ?: moduleMap[projectData.internalName + "." + moduleName]
            ?: moduleMap[projectData.internalName]
            ?: return

        val module = moduleMap[rootModule.name + "-forge"] ?: rootModule

        if (SemanticVersion.parse(forgeVersion) < ForgeModuleType.FG3_VERSION) {
            manualCreate(project, moduleMap, module, size)
        } else {
            genIntellijRuns(project, moduleMap, module, size, task)
        }
    }

    private fun manualCreate(project: Project, moduleMap: Map<String, Module>, module: Module, size: Int) {
        invokeLater {
            val mainModule = findMainModule(moduleMap, module, size)

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

            if (size == 1) {
                runClientConfig.setModule(mainModule)
            } else {
                runClientConfig.setModule(mainModule)
            }
            clientSettings.isActivateToolWindowBeforeRun = true

            runManager.addConfiguration(clientSettings, false)
            runManager.selectedConfiguration = clientSettings

            // Server run config
            val serverSettings = runManager.createConfiguration(module.name + " run server", factory)
            val runServerConfig = serverSettings.configuration as ApplicationConfiguration
            runServerConfig.isAllowRunningInParallel = false

            runServerConfig.mainClassName = "GradleStartServer"
            runServerConfig.programParameters = "nogui"
            runServerConfig.workingDirectory = project.basePath + File.separator + "run"

            if (size == 1) {
                runServerConfig.setModule(mainModule)
            } else {
                runServerConfig.setModule(mainModule)
            }

            serverSettings.isActivateToolWindowBeforeRun = true
            runManager.addConfiguration(serverSettings, false)
        }
    }

    private fun genIntellijRuns(
        project: Project,
        moduleMap: Map<String, Module>,
        module: Module,
        size: Int,
        task: String
    ) {
        val mainModule = findMainModule(moduleMap, module, size)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "genIntellijRuns", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                val projectDir = project.guessProjectDir() ?: return
                runGradleTask(project, projectDir.localFile, indicator) { launcher ->
                    launcher.forTasks(task)
                }

                cleanupGeneratedRuns(project, mainModule)
            }
        })
    }

    private fun cleanupGeneratedRuns(project: Project, module: Module) {
        invokeAndWait {
            ForgeRunManagerListener(module)
        }

        project.guessProjectDir()?.let { dir ->
            LocalFileSystem.getInstance().refreshFiles(listOf(dir), true, true, null)
        }
    }

    private fun findMainModule(moduleMap: Map<String, Module>, module: Module, size: Int): Module {
        return if (size == 1) {
            moduleMap[module.name + "_main"] ?: moduleMap[module.name + ".main"]
        } else {
            moduleMap[module.name + "-forge_main"] ?: moduleMap[module.name + "-forge.main"]
        } ?: module
    }
}

class ForgeRunManagerListener(private val module: Module) : RunManagerListener {
    private val count = AtomicInteger(3)
    private val disposable = Disposer.newDisposable()

    init {
        Disposer.register(module, disposable)
        module.project.messageBus.connect(disposable).subscribe(RunManagerListener.TOPIC, this)
    }

    override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
        val config = settings.configuration as? ApplicationConfiguration ?: return

        when (settings.name) {
            "runClient", "runServer", "runData" -> {}
            else -> return
        }

        config.isAllowRunningInParallel = false
        config.setModule(module)
        RunManager.getInstance(module.project).addConfiguration(settings)

        if (count.decrementAndGet() == 0) {
            Disposer.dispose(disposable)
        }
    }
}
