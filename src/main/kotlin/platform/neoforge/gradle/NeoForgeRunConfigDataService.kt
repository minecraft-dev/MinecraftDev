/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.neoforge.gradle

import com.demonwav.mcdev.platform.neoforge.creator.MAGIC_RUN_CONFIGS_FILE
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.localFile
import com.demonwav.mcdev.util.runGradleTaskAndWait
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
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
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import org.jetbrains.plugins.gradle.util.GradleConstants

class NeoForgeRunConfigDataService : AbstractProjectDataService<ProjectData, Project>() {

    override fun getTargetDataKey() = ProjectKeys.PROJECT

    override fun postProcess(
        toImport: Collection<DataNode<ProjectData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider,
    ) {
        if (projectData == null || projectData.owner != GradleConstants.SYSTEM_ID) {
            return
        }

        val baseDir = project.guessProjectDir() ?: return
        val baseDirPath = baseDir.localFile.toPath()
        val hello = baseDirPath.resolve(Paths.get(".gradle", MAGIC_RUN_CONFIGS_FILE))
        if (!Files.isRegularFile(hello)) {
            return
        }

        val lines = Files.readAllLines(hello, Charsets.UTF_8)
        if (lines.size < 4) {
            return
        }

        val (moduleName, mcVersion, forgeVersion, task) = lines

        val moduleMap = modelsProvider.modules.associateBy { it.name }
        val module = moduleMap[moduleName] ?: return

        // We've found the module we were expecting, so we can assume the project imported correctly
        Files.delete(hello)

        genIntellijRuns(project, moduleMap, module, task)
    }

    private fun genIntellijRuns(
        project: Project,
        moduleMap: Map<String, Module>,
        module: Module,
        task: String,
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

                    cleanupGeneratedRuns(project, mainModule)
                }
            },
        )
    }

    private fun cleanupGeneratedRuns(project: Project, module: Module) {
        invokeAndWait {
            if (!module.isDisposed) {
                NeoForgeRunManagerListener(module, true)
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

class NeoForgeRunManagerListener(private val module: Module, hasData: Boolean) : RunManagerListener {
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
