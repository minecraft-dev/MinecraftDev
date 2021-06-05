/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
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
import com.demonwav.mcdev.creator.buildsystem.maven.CommonModuleDependencyStep
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.MavenGitignoreStep
import com.intellij.openapi.module.Module
import java.nio.file.Path

sealed class Sponge8ProjectCreator<T : BuildSystem>(
    protected val rootDirectory: Path,
    protected val rootModule: Module,
    protected val buildSystem: T,
    protected val config: SpongeProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

    protected fun setupDependencyStep(): SpongeDependenciesSetup {
        val spongeApiVersion = config.spongeApiVersion
        return SpongeDependenciesSetup(buildSystem, spongeApiVersion)
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

    override fun getSingleModuleSteps(): Iterable<CreatorStep> {
        val mainClassStep = setupMainClassStep()

        val pomText = SpongeTemplate.applyPom(project)

        return listOf(
            setupDependencyStep(),
            BasicMavenStep(project, rootDirectory, buildSystem, config, pomText),
            mainClassStep,
            MavenGitignoreStep(project, rootDirectory),
            BasicMavenFinalizerStep(rootModule, rootDirectory)
        )
    }

    override fun getMultiModuleSteps(projectBaseDir: Path): Iterable<CreatorStep> {
        val depStep = setupDependencyStep()
        val commonDepStep = CommonModuleDependencyStep(buildSystem)
        val mainClassStep = setupMainClassStep()

        val pomText = SpongeTemplate.applySubPom(project)
        val mavenStep = BasicMavenStep(
            project = project,
            rootDirectory = rootDirectory,
            buildSystem = buildSystem,
            config = config,
            pomText = pomText,
            parts = listOf(
                BasicMavenStep.setupDirs(),
                BasicMavenStep.setupSubCore(buildSystem.parentOrError.artifactId),
                BasicMavenStep.setupSubName(config.type),
                BasicMavenStep.setupInfo(),
                BasicMavenStep.setupDependencies()
            )
        )
        return listOf(depStep, commonDepStep, mavenStep, mainClassStep)
    }
}

class Sponge8GradleCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: GradleBuildSystem,
    config: SpongeProjectConfig
) : Sponge8ProjectCreator<GradleBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSingleModuleSteps(): Iterable<CreatorStep> {
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

    override fun getMultiModuleSteps(projectBaseDir: Path): Iterable<CreatorStep> {
        val mainClassStep = setupMainClassStep()

        val buildText = Sponge8Template.applySubBuildGradle(
            project,
            buildSystem,
            config
        )
        val files = GradleFiles(buildText, null, null)

        return listOf(
            CreateDirectoriesStep(buildSystem, rootDirectory),
            GradleSetupStep(project, rootDirectory, buildSystem, files, true),
            mainClassStep
        )
    }
}
