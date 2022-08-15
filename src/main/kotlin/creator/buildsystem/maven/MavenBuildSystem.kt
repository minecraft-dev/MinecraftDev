/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem.maven

import com.demonwav.mcdev.creator.CreateDirectoriesStep
import com.demonwav.mcdev.creator.CreatorStep
import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.BuildSystemTemplate
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.module.Module
import java.nio.file.Path
import java.util.Locale

class MavenBuildSystem(
    groupId: String,
    artifactId: String,
    version: String,
    override val parent: MavenBuildSystem? = null
) : BuildSystem(groupId, artifactId, version) {

    override val type = BuildSystemType.MAVEN

    override fun createSub(artifactId: String): BuildSystem {
        return MavenBuildSystem(this.groupId, artifactId, this.version, this)
    }

    override fun multiModuleBaseSteps(
        module: Module,
        types: List<PlatformType>,
        rootDirectory: Path
    ): Iterable<CreatorStep> {
        val project = module.project
        val pomText = BuildSystemTemplate.applyPom(project)

        val baseName = artifactId.lowercase(Locale.ENGLISH)
        val moduleNames = mutableListOf("$baseName-common")
        moduleNames += types.map { "$baseName-${it.name.lowercase(Locale.ENGLISH)}" }

        return listOf(
            BasicMavenStep(
                project = project,
                rootDirectory = rootDirectory,
                buildSystem = this,
                config = null,
                pomText = pomText,
                parts = listOf(BasicMavenStep.setupCore(), BasicMavenStep.setupModules(moduleNames))
            )
        )
    }

    override fun multiModuleBaseFinalizer(module: Module, rootDirectory: Path): Iterable<CreatorStep> {
        return listOf(
            MavenGitignoreStep(module.project, rootDirectory),
            BasicMavenFinalizerStep(module, rootDirectory)
        )
    }

    override fun multiModuleCommonSteps(module: Module, rootDirectory: Path): Iterable<CreatorStep> {
        val project = module.project
        val pomText = BuildSystemTemplate.applyCommonPom(project)
        return listOf(
            CreateDirectoriesStep(this, rootDirectory),
            BasicMavenStep(
                project = project,
                rootDirectory = rootDirectory,
                buildSystem = this,
                config = null,
                pomText = pomText,
                parts = listOf(BasicMavenStep.setupSubCore(this.parentOrError.artifactId))
            )
        )
    }

    override fun buildCreator(obj: Any, rootDirectory: Path, module: Module): ProjectCreator {
        if (obj !is MavenCreator) {
            throw IllegalStateException("Cannot create a Maven module from ${obj.javaClass.name}")
        }
        return obj.buildMavenCreator(rootDirectory, module, this)
    }

    override fun configure(config: ProjectConfig, rootDirectory: Path) {
        if (config is MavenCreator) {
            config.configureRootMaven(rootDirectory, this)
        }
    }
}

/**
 * A [ProjectConfig][com.demonwav.mcdev.creator.ProjectConfig] must implement this interface in order to support
 * creating `Maven` projects.
 */
interface MavenCreator {
    fun buildMavenCreator(rootDirectory: Path, module: Module, buildSystem: MavenBuildSystem): ProjectCreator

    /**
     * This method allows extra configuration of the root [MavenBuildSystem] before any [CreatorStep]s have been
     * created or executed. Not typically needed.
     */
    fun configureRootMaven(rootDirectory: Path, buildSystem: MavenBuildSystem) {}
}
