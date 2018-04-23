/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.buildsystem.BuildDependency
import com.demonwav.mcdev.buildsystem.BuildRepository
import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration
import com.demonwav.mcdev.platform.canary.CanaryProjectConfiguration
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
import com.google.common.base.MoreObjects
import com.google.common.base.Objects
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile

class MinecraftProjectCreator {

    lateinit var root: VirtualFile
    lateinit var groupId: String
    lateinit var artifactId: String
    lateinit var version: String
    lateinit var module: Module
    lateinit var buildSystem: BuildSystem

    val settings = LinkedHashMap<PlatformType, ProjectConfiguration>()

    private lateinit var sourceDir: VirtualFile
    private lateinit var resourceDir: VirtualFile
    private lateinit var testDir: VirtualFile
    private lateinit var pomFile: VirtualFile

    fun create() {
        buildSystem.rootDirectory = root

        buildSystem.groupId = groupId
        buildSystem.artifactId = artifactId
        buildSystem.version = version

        buildSystem.pluginName = settings.values.iterator().next().pluginName

        if (settings.size == 1) {
            doSingleModuleCreate()
        } else {
            doMultiModuleCreate()
        }
    }

    private fun doSingleModuleCreate() {
        val configuration = settings.values.iterator().next()
        addDependencies(configuration, buildSystem)

        ProgressManager.getInstance().run(object : Task.Backgroundable(module.project, "Setting Up Project", false) {
            override fun shouldStartInBackground() = false

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                buildSystem.create(module.project, configuration, indicator)
                configuration.create(module.project, buildSystem, indicator)
                configuration.type.type.performCreationSettingSetup(module.project)
                buildSystem.finishSetup(module, listOf(configuration), indicator)
            }
        })
    }

    private fun doMultiModuleCreate() {
        if (buildSystem !is GradleBuildSystem) {
            throw IllegalStateException("BuildSystem must be Gradle")
        }

        val gradleBuildSystem = buildSystem as GradleBuildSystem?
        ProgressManager.getInstance().run(object : Task.Backgroundable(module.project, "Setting Up Project", false) {
            override fun shouldStartInBackground() = false

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                val map = gradleBuildSystem!!.createMultiModuleProject(module.project, settings, indicator)

                map.forEach { g, p ->
                    p.create(module.project, g, indicator)
                    p.type.type.performCreationSettingSetup(module.project)
                }
                gradleBuildSystem.finishSetup(module, map.values, indicator)
            }
        })
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .add("root", root)
            .add("groupId", groupId)
            .add("artifactId", artifactId)
            .add("version", version)
            .add("module", module)
            .add("buildSystem", buildSystem)
            .add("settings", settings)
            .add("sourceDir", sourceDir)
            .add("resourceDir", resourceDir)
            .add("testDir", testDir)
            .add("pomFile", pomFile)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as? MinecraftProjectCreator ?: return false
        return Objects.equal(root, that.root) &&
            Objects.equal(groupId, that.groupId) &&
            Objects.equal(artifactId, that.artifactId) &&
            Objects.equal(version, that.version) &&
            Objects.equal(module, that.module) &&
            Objects.equal(buildSystem, that.buildSystem) &&
            Objects.equal(settings, that.settings) &&
            Objects.equal(sourceDir, that.sourceDir) &&
            Objects.equal(resourceDir, that.resourceDir) &&
            Objects.equal(testDir, that.testDir) &&
            Objects.equal(pomFile, that.pomFile)
    }

    override fun hashCode(): Int {
        return Objects.hashCode(root, groupId, artifactId, version, module,
                                buildSystem, settings, sourceDir, resourceDir, testDir, pomFile)
    }

    companion object {
        fun addDependencies(configuration: ProjectConfiguration,
                            buildSystem: BuildSystem) {
            // Forge doesn't have a dependency like this
            if (configuration.type === PlatformType.FORGE) {
                return
            }

            val buildRepository = BuildRepository()
            val buildDependency = BuildDependency()

            // Sponge projects using Gradle use SpongeGradle which automatically adds the required repositories
            if (configuration.type !== PlatformType.SPONGE || buildSystem !is GradleBuildSystem) {
                buildSystem.repositories.add(buildRepository)
            }

            buildSystem.dependencies.add(buildDependency)
            when (configuration.type) {
                PlatformType.BUKKIT -> {
                    buildRepository.id = "spigotmc-repo"
                    buildRepository.url = "https://hub.spigotmc.org/nexus/content/groups/public/"
                    buildDependency.groupId = "org.bukkit"
                    buildDependency.artifactId = "bukkit"
                    buildDependency.version = (configuration as BukkitProjectConfiguration).minecraftVersion + "-R0.1-SNAPSHOT"
                }
                PlatformType.SPIGOT -> {
                    buildRepository.id = "spigotmc-repo"
                    buildRepository.url = "https://hub.spigotmc.org/nexus/content/groups/public/"
                    buildDependency.groupId = "org.spigotmc"
                    buildDependency.artifactId = "spigot-api"
                    buildDependency.version = (configuration as BukkitProjectConfiguration).minecraftVersion + "-R0.1-SNAPSHOT"
                    addSonatype(buildSystem.repositories)
                }
                PlatformType.PAPER -> {
                    buildRepository.id = "destroystokyo-repo"
                    buildRepository.url = "https://repo.destroystokyo.com/repository/maven-public/"
                    buildDependency.groupId = "com.destroystokyo.paper"
                    buildDependency.artifactId = "paper-api"
                    buildDependency.version = (configuration as BukkitProjectConfiguration).minecraftVersion + "-R0.1-SNAPSHOT"
                    addSonatype(buildSystem.repositories)
                }
                PlatformType.BUNGEECORD -> {
                    buildRepository.id = "sonatype-oss-repo"
                    buildRepository.url = "https://oss.sonatype.org/content/groups/public/"
                    buildDependency.groupId = "net.md-5"
                    buildDependency.artifactId = "bungeecord-api"
                    buildDependency.version = (configuration as BungeeCordProjectConfiguration).minecraftVersion + "-SNAPSHOT"
                }
                PlatformType.WATERFALL -> {
                    buildRepository.id = "destroystokyo-repo"
                    buildRepository.url = "https://repo.destroystokyo.com/repository/maven-public/"
                    buildDependency.groupId = "io.github.waterfallmc"
                    buildDependency.artifactId = "waterfall-api"
                    buildDependency.version = (configuration as BungeeCordProjectConfiguration).minecraftVersion + "-SNAPSHOT"
                    addSonatype(buildSystem.repositories)
                }
                PlatformType.SPONGE -> {
                    buildRepository.id = "spongepowered-repo"
                    buildRepository.url = "https://repo.spongepowered.org/maven/"
                    buildDependency.groupId = "org.spongepowered"
                    buildDependency.artifactId = "spongeapi"
                    if (configuration is SpongeProjectConfiguration) {
                        buildDependency.version = configuration.spongeApiVersion
                    } else {
                        buildDependency.version = (configuration as SpongeForgeProjectConfiguration).spongeApiVersion
                    }
                }
                PlatformType.CANARY -> {
                    if (!(configuration as CanaryProjectConfiguration).canaryVersion.endsWith("-SNAPSHOT")) {
                        buildRepository.id = "vi-releases"
                        buildRepository.url = "http://repo.visualillusionsent.net:8888/repository/internal/"
                    } else {
                        buildRepository.id = "vi-snapshots"
                        buildRepository.url = "http://repo.visualillusionsent.net:8888/repository/snapshots/"
                    }
                    buildDependency.groupId = "net.canarymod"
                    buildDependency.artifactId = "CanaryLib"
                    buildDependency.version = configuration.canaryVersion
                }
                PlatformType.NEPTUNE -> {
                    if (!(configuration as CanaryProjectConfiguration).canaryVersion.endsWith("-SNAPSHOT")) {
                        buildRepository.id = "lex-releases"
                        buildRepository.url = "https://repo.lexteam.xyz/maven/releases/"
                    } else {
                        buildRepository.id = "lex-snapshots"
                        buildRepository.url = "https://repo.lexteam.xyz/maven/snapshots/"
                    }
                    addVIRepo(buildSystem.repositories)
                    buildDependency.groupId = "org.neptunepowered"
                    buildDependency.artifactId = "NeptuneLib"
                    buildDependency.version = configuration.canaryVersion
                }
                else -> {}
            }
            buildDependency.scope = "provided"
        }

        private fun addSonatype(buildRepositories: MutableList<BuildRepository>) {
            buildRepositories.add(BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/"))
        }

        private fun addVIRepo(buildRepositories: MutableList<BuildRepository>) {
            buildRepositories.add(BuildRepository("vi-releases", "http://repo.visualillusionsent.net:8888/repository/internal/"))
            buildRepositories.add(BuildRepository("vi-snapshots", "http://repo.visualillusionsent.net:8888/repository/snapshots/"))
        }
    }
}
