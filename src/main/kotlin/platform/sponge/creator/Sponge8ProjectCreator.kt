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
            Sponge8Template.applyMainClass(project, buildSystem.artifactId, packageName, className)
        }
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
            config.spongeApiVersion,
            config.pluginName,
            config.mainClass,
            config.description,
            config.website,
            config.authors,
            config.dependencies
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
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem)
        )
    }

    override fun getMultiModuleSteps(projectBaseDir: Path): Iterable<CreatorStep> {
        val mainClassStep = setupMainClassStep()

        val buildText = Sponge8Template.applySubBuildGradle(
            project,
            buildSystem,
            config.spongeApiVersion,
            config.pluginName,
            config.mainClass,
            config.description,
            config.website,
            config.authors,
            config.dependencies
        )
        val files = GradleFiles(buildText, null, null)

        return listOf(
            CreateDirectoriesStep(buildSystem, rootDirectory),
            GradleSetupStep(project, rootDirectory, buildSystem, files, true),
            mainClassStep
        )
    }
}
