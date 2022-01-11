/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.nukkit.creator

import com.demonwav.mcdev.creator.BaseProjectCreator
import com.demonwav.mcdev.creator.BasicJavaClassStep
import com.demonwav.mcdev.creator.CreateDirectoriesStep
import com.demonwav.mcdev.creator.CreatorStep
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
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
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.nio.file.Path

sealed class NukkitProjectCreator<T : BuildSystem>(
    protected val rootDirectory: Path,
    protected val rootModule: Module,
    protected val buildSystem: T,
    protected val config: NukkitProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

    protected fun setupMainClassStep(): BasicJavaClassStep {
        return createJavaClassStep(config.mainClass) { packageName, className ->
            NukkitTemplate.applyMainClass(project, packageName, className)
        }
    }

    protected fun setupDependencyStep(): NukkitDependenciesStep {
        val mcVersion = config.minecraftVersion
        return NukkitDependenciesStep(buildSystem, config.type, mcVersion)
    }

    protected fun setupYmlStep(): NukkitPluginYmlStep {
        return NukkitPluginYmlStep(project, buildSystem, config)
    }
}

class NukkitMavenCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: MavenBuildSystem,
    config: NukkitProjectConfig
) : NukkitProjectCreator<MavenBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSingleModuleSteps(): Iterable<CreatorStep> {
        val pomText = NukkitTemplate.applyPom(project)
        return listOf(
            setupDependencyStep(),
            BasicMavenStep(project, rootDirectory, buildSystem, config, pomText),
            setupMainClassStep(),
            setupYmlStep(),
            MavenGitignoreStep(project, rootDirectory),
            BasicMavenFinalizerStep(rootModule, rootDirectory)
        )
    }

    override fun getMultiModuleSteps(projectBaseDir: Path): Iterable<CreatorStep> {
        val depStep = setupDependencyStep()
        val commonDepStep = CommonModuleDependencyStep(buildSystem)
        val mainClassStep = setupMainClassStep()
        val ymlStep = setupYmlStep()

        val pomText = NukkitTemplate.applySubPom(project)
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
        return listOf(depStep, commonDepStep, mavenStep, mainClassStep, ymlStep)
    }
}

class NukkitGradleCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: GradleBuildSystem,
    config: NukkitProjectConfig
) : NukkitProjectCreator<GradleBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSingleModuleSteps(): Iterable<CreatorStep> {
        val buildText = NukkitTemplate.applyBuildGradle(project, buildSystem, config)
        val propText = NukkitTemplate.applyGradleProp(project)
        val settingsText = NukkitTemplate.applySettingsGradle(project, buildSystem.artifactId)
        val files = GradleFiles(buildText, propText, settingsText)

        return listOf(
            setupDependencyStep(),
            CreateDirectoriesStep(buildSystem, rootDirectory),
            GradleSetupStep(project, rootDirectory, buildSystem, files),
            setupMainClassStep(),
            setupYmlStep(),
            GradleWrapperStep(project, rootDirectory, buildSystem),
            GradleGitignoreStep(project, rootDirectory),
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem)
        )
    }

    override fun getMultiModuleSteps(projectBaseDir: Path): Iterable<CreatorStep> {
        val buildText = NukkitTemplate.applySubBuildGradle(project, buildSystem)
        val files = GradleFiles(buildText, null, null)

        return listOf(
            setupDependencyStep(),
            CreateDirectoriesStep(buildSystem, rootDirectory),
            GradleSetupStep(project, rootDirectory, buildSystem, files),
            setupMainClassStep(),
            setupYmlStep()
        )
    }
}

open class NukkitDependenciesStep(
    protected val buildSystem: BuildSystem,
    protected val type: PlatformType,
    protected val apiVersion: String
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        nukkitRepo(buildSystem.repositories)
        buildSystem.dependencies.add(
            BuildDependency(
                "cn.nukkit",
                "nukkit",
                "$apiVersion-SNAPSHOT",
                mavenScope = "provided",
                gradleConfiguration = "compileOnly"
            )
        )
    }

    private fun nukkitRepo(list: MutableList<BuildRepository>) {
        list.add(
            BuildRepository(
                "opencollab-repo-snapshot",
                "https://repo.opencollab.dev/maven-snapshots/"
            )
        )
    }
}

class NukkitPluginYmlStep(
    private val project: Project,
    private val buildSystem: BuildSystem,
    private val config: NukkitProjectConfig
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val text = NukkitTemplate.applyPluginYml(project, config, buildSystem)
        CreatorStep.writeTextToFile(project, buildSystem.dirsOrError.resourceDirectory, "plugin.yml", text)
    }
}
