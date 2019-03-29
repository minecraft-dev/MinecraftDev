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
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile

class MinecraftProjectCreator {

    var buildSystem: BuildSystem? = null

    val configs = LinkedHashSet<ProjectConfiguration>()

    fun create(root: VirtualFile, module: Module) {
        val build = buildSystem ?: return

        if (configs.size == 1) {
            doSingleModuleCreate(root, build, module)
        } else {
            doMultiModuleCreate(root, build, module)
        }
    }

    private fun doSingleModuleCreate(rootDirectory: VirtualFile, buildSystem: BuildSystem, module: Module) {
        if (configs.isEmpty()) {
            return
        }
        val configuration = configs.first()
        addDependencies(configuration, buildSystem)

        ProgressManager.getInstance().run(object : Task.Backgroundable(module.project, "Setting Up Project", false) {
            override fun shouldStartInBackground() = false

            override fun run(indicator: ProgressIndicator) {
                if (module.isDisposed || module.project.isDisposed) {
                    return
                }
                indicator.isIndeterminate = true

                val pluginName = configuration.base?.pluginName ?: return
                buildSystem.create(module.project, rootDirectory, configuration, indicator, pluginName)
                configuration.create(module.project, buildSystem, indicator)
                configuration.type.type.performCreationSettingSetup(module.project)
                buildSystem.finishSetup(module, rootDirectory, configs, indicator)
            }
        })
    }

    private fun doMultiModuleCreate(rootDirectory: VirtualFile, buildSystem: BuildSystem, module: Module) {
        if (buildSystem !is GradleBuildSystem) {
            throw IllegalStateException("BuildSystem must be Gradle")
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(module.project, "Setting Up Project", false) {
            override fun shouldStartInBackground() = false

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                val pluginName = configs.firstOrNull()?.base?.pluginName ?: return
                val map = buildSystem.createMultiModuleProject(
                    rootDirectory,
                    module.project,
                    configs,
                    indicator,
                    pluginName
                )

                map.forEach { g, p ->
                    p.create(module.project, g, indicator)
                    p.type.type.performCreationSettingSetup(module.project)
                }
                buildSystem.finishSetup(module, rootDirectory, map.values, indicator)
            }
        })
    }

    companion object {
        fun addDependencies(
            configuration: ProjectConfiguration,
            buildSystem: BuildSystem
        ) {
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
                    val mcVers = (configuration as BukkitProjectConfiguration).data?.minecraftVersion ?: return
                    buildDependency.version = "$mcVers-R0.1-SNAPSHOT"
                }
                PlatformType.SPIGOT -> {
                    buildRepository.id = "spigotmc-repo"
                    buildRepository.url = "https://hub.spigotmc.org/nexus/content/groups/public/"
                    buildDependency.groupId = "org.spigotmc"
                    buildDependency.artifactId = "spigot-api"
                    val mcVers = (configuration as BukkitProjectConfiguration).data?.minecraftVersion ?: return
                    buildDependency.version = "$mcVers-R0.1-SNAPSHOT"
                    addSonatype(buildSystem.repositories)
                }
                PlatformType.PAPER -> {
                    buildRepository.id = "destroystokyo-repo"
                    buildRepository.url = "https://repo.destroystokyo.com/repository/maven-public/"
                    buildDependency.groupId = "com.destroystokyo.paper"
                    buildDependency.artifactId = "paper-api"
                    val mcVers = (configuration as BukkitProjectConfiguration).data?.minecraftVersion ?: return
                    buildDependency.version = "$mcVers-R0.1-SNAPSHOT"
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
                else -> {}
            }
            buildDependency.scope = "provided"
        }

        private fun addSonatype(buildRepositories: MutableList<BuildRepository>) {
            buildRepositories.add(BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/"))
        }
    }
}
