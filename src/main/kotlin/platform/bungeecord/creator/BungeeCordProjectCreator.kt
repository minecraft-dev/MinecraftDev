/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.creator

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
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.MavenGitignoreStep
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.creator.BukkitDependenciesStep
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.nio.file.Path

sealed class BungeeCordProjectCreator<T : BuildSystem>(
    protected val rootDirectory: Path,
    protected val rootModule: Module,
    protected val buildSystem: T,
    protected val config: BungeeCordProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

    protected fun setupMainClassStep(): BasicJavaClassStep {
        return createJavaClassStep(config.mainClass) { packageName, className ->
            BungeeCordTemplate.applyMainClass(project, packageName, className)
        }
    }

    protected fun setupDependencyStep(): BungeeCordDependenciesStep {
        val mcVersion = config.minecraftVersion
        return BungeeCordDependenciesStep(buildSystem, config.type, mcVersion)
    }

    protected fun setupYmlStep(): BungeeYmlStep {
        return BungeeYmlStep(project, buildSystem, config)
    }
}

class BungeeCordMavenCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: MavenBuildSystem,
    config: BungeeCordProjectConfig
) : BungeeCordProjectCreator<MavenBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSteps(): Iterable<CreatorStep> {
        val pomText = BungeeCordTemplate.applyPom(project)
        return listOf(
            setupDependencyStep(),
            BasicMavenStep(project, rootDirectory, buildSystem, config, pomText),
            setupMainClassStep(),
            setupYmlStep(),
            MavenGitignoreStep(project, rootDirectory),
            BasicMavenFinalizerStep(rootModule, rootDirectory)
        )
    }
}

class BungeeCordGradleCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: GradleBuildSystem,
    config: BungeeCordProjectConfig
) : BungeeCordProjectCreator<GradleBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSteps(): Iterable<CreatorStep> {
        val buildText = BungeeCordTemplate.applyBuildGradle(project, buildSystem)
        val projectText = BungeeCordTemplate.applyGradleProp(project)
        val settingsText = BungeeCordTemplate.applySettingsGradle(project, buildSystem.artifactId)
        val files = GradleFiles(buildText, projectText, settingsText)

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
}

class BungeeCordDependenciesStep(
    buildSystem: BuildSystem,
    type: PlatformType,
    mcVersion: String
) : BukkitDependenciesStep(buildSystem, type, mcVersion) {
    override fun runStep(indicator: ProgressIndicator) {
        addSonatype(buildSystem.repositories)
        when (type) {
            PlatformType.WATERFALL -> {
                buildSystem.repositories.add(
                    BuildRepository(
                        "papermc-repo",
                        "https://repo.papermc.io/repository/maven-public/"
                    )
                )
                buildSystem.dependencies.add(
                    BuildDependency(
                        "io.github.waterfallmc",
                        "waterfall-api",
                        "$mcVersion-SNAPSHOT",
                        mavenScope = "provided",
                        gradleConfiguration = "compileOnly"
                    )
                )
            }
            PlatformType.BUNGEECORD -> {
                buildSystem.dependencies.add(
                    BuildDependency(
                        "net.md-5",
                        "bungeecord-api",
                        mcVersion,
                        mavenScope = "provided",
                        gradleConfiguration = "compileOnly"
                    )
                )
            }
            else -> {
            }
        }
    }
}

class BungeeYmlStep(
    private val project: Project,
    private val buildSystem: BuildSystem,
    private val config: BungeeCordProjectConfig
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val text = BungeeCordTemplate.applyBungeeYml(project, config, buildSystem)
        CreatorStep.writeTextToFile(project, buildSystem.dirsOrError.resourceDirectory, "bungee.yml", text)
    }
}
