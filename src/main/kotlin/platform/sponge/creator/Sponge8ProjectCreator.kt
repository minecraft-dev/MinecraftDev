/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.BaseProjectCreator
import com.demonwav.mcdev.creator.BasicJavaClassStep
import com.demonwav.mcdev.creator.CreateDirectoriesStep
import com.demonwav.mcdev.creator.CreatorStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.BasicGradleFinalizerStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleFiles
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleGitignoreStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleSetupStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.maven.BasicMavenFinalizerStep
import com.demonwav.mcdev.creator.buildsystem.maven.BasicMavenStep
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.MavenGitignoreStep
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.nio.file.Path

sealed class Sponge8ProjectCreator<T : BuildSystem>(
    protected val rootDirectory: Path,
    protected val rootModule: Module,
    protected val buildSystem: T,
    protected val config: SpongeProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

    protected fun setupDependencyStep(): SpongeDependenciesSetup {
        val spongeApiVersion = config.spongeApiVersion
        return SpongeDependenciesSetup(buildSystem, spongeApiVersion, false)
    }

    protected fun setupMainClassStep(): BasicJavaClassStep {
        return createJavaClassStep(config.mainClass) { packageName, className ->
            val pluginId = (buildSystem.parent ?: buildSystem).artifactId
            Sponge8Template.applyMainClass(project, pluginId, packageName, className)
        }
    }
}

class Sponge8MavenCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: MavenBuildSystem,
    config: SpongeProjectConfig
) : Sponge8ProjectCreator<MavenBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSteps(): Iterable<CreatorStep> {
        val mainClassStep = setupMainClassStep()
        val pluginsJsonStep = CreatePluginsJsonStep(project, buildSystem, config)

        val pomText = SpongeTemplate.applyPom(project, config)

        return listOf(
            setupDependencyStep(),
            BasicMavenStep(project, rootDirectory, buildSystem, config, pomText),
            mainClassStep,
            pluginsJsonStep,
            MavenGitignoreStep(project, rootDirectory),
            BasicMavenFinalizerStep(rootModule, rootDirectory),
        )
    }
}

class CreatePluginsJsonStep(
    private val project: Project,
    private val buildSystem: BuildSystem,
    private val config: SpongeProjectConfig
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val pluginsJsonPath = buildSystem.dirsOrError.resourceDirectory.resolve("META-INF")
        val pluginsJsonText = Sponge8Template.applyPluginsJson(project, buildSystem, config)
        CreatorStep.writeTextToFile(project, pluginsJsonPath, "sponge_plugins.json", pluginsJsonText)
    }
}

class Sponge8GradleCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: GradleBuildSystem,
    config: SpongeProjectConfig
) : Sponge8ProjectCreator<GradleBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSteps(): Iterable<CreatorStep> {
        val mainClassStep = setupMainClassStep()

        val buildText = Sponge8Template.applyBuildGradle(
            project,
            buildSystem,
            config
        )
        val propText = Sponge8Template.applyGradleProp(project)
        val settingsText = Sponge8Template.applySettingsGradle(project, buildSystem.artifactId)
        val files = GradleFiles(buildText, propText, settingsText)

        return listOf(
            CreateDirectoriesStep(buildSystem, rootDirectory),
            GradleSetupStep(project, rootDirectory, buildSystem, files, true),
            mainClassStep,
            GradleWrapperStep(project, rootDirectory, buildSystem),
            GradleGitignoreStep(project, rootDirectory),
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem, "runServer")
        )
    }
}
