/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem

import com.demonwav.mcdev.creator.CreatorStep
import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.MavenCreator
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.module.Module
import java.nio.file.Path
import java.util.EnumSet
import kotlin.reflect.KClass

/**
 * Base class for build system project creation.
 */
abstract class BuildSystem(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    open val parent: BuildSystem? = null
    val parentOrError: BuildSystem
        get() = parent ?: throw IllegalStateException("Sub-module build system does not have a parent")

    abstract val type: BuildSystemType

    abstract fun buildCreator(obj: Any, rootDirectory: Path, module: Module): ProjectCreator
    open fun configure(config: ProjectConfig, rootDirectory: Path) {}

    var repositories: MutableList<BuildRepository> = mutableListOf()
    var dependencies: MutableList<BuildDependency> = mutableListOf()

    var directories: DirectorySet? = null
    val dirsOrError: DirectorySet
        get() = directories ?: throw IllegalStateException("Project structure is not yet created")

    val commonModuleName: String
        get() = parentOrError.artifactId + "-common"

    /**
     * Initial [CreatorStep]s to execute when creating the base of a multi-module project for this build system type.
     * These steps run before the platform-specific submodule steps run.
     *
     * @see com.demonwav.mcdev.creator.MinecraftProjectCreator.CreateTask.run
     */
    abstract fun multiModuleBaseSteps(
        module: Module,
        types: List<PlatformType>,
        rootDirectory: Path
    ): Iterable<CreatorStep>

    /**
     * Finalizer [CreatorStep]s to execute when finishing a multi-module project for this build system type. These steps
     * are run after the platform-specific submodule steps are run, as well as after [multiModuleCommonSteps].
     *
     * @see com.demonwav.mcdev.creator.MinecraftProjectCreator.CreateTask.run
     */
    abstract fun multiModuleBaseFinalizer(
        module: Module,
        rootDirectory: Path
    ): Iterable<CreatorStep>

    /**
     * [CreatorStep]s for creating the shared common module for a multi-module project for this build system type. These
     * steps run after the platform-specific submodule steps are run, and before [multiModuleBaseFinalizer].
     *
     * @see com.demonwav.mcdev.creator.MinecraftProjectCreator.CreateTask.run
     */
    abstract fun multiModuleCommonSteps(
        module: Module,
        rootDirectory: Path
    ): Iterable<CreatorStep>

    /**
     * Using the given [artifactId] create a new child [BuildSystem] instance which shares all of the other properties
     * as this instance, except for the `artifactId`.
     */
    abstract fun createSub(artifactId: String): BuildSystem
}

enum class BuildSystemType(private val readableName: String, val creatorType: KClass<*>) {
    MAVEN("Maven", MavenCreator::class) {
        override fun create(groupId: String, artifactId: String, version: String): BuildSystem {
            return MavenBuildSystem(groupId, artifactId, version)
        }
    },
    GRADLE("Gradle", GradleCreator::class) {
        override fun create(groupId: String, artifactId: String, version: String): BuildSystem {
            return GradleBuildSystem(groupId, artifactId, version)
        }
    };

    /**
     * Create a new [BuildSystem] instance using the provided artifact definition.
     */
    abstract fun create(groupId: String, artifactId: String, version: String): BuildSystem

    override fun toString(): String {
        return readableName
    }
}

data class BuildDependency(
    val groupId: String = "",
    val artifactId: String = "",
    val version: String = "",
    val mavenScope: String? = null,
    val gradleConfiguration: String? = null
)

data class BuildRepository(
    var id: String = "",
    var url: String = "",
    val buildSystems: EnumSet<BuildSystemType> = EnumSet.allOf(BuildSystemType::class.java)
)
