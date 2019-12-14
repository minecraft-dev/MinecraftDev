/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

@file:Suppress("Duplicates")

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.buildsystem.BuildDependency
import com.demonwav.mcdev.buildsystem.BuildRepository
import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

class BukkitProjectConfiguration(override var type: PlatformType) : ProjectConfiguration(), BukkitLikeConfiguration {

    data class BukkitData(
        val loadOrder: LoadOrder = LoadOrder.POSTWORLD,
        val loadBefore: MutableList<String> = mutableListOf(),
        val prefix: String = "",
        val minecraftVersion: String = ""
    )

    var data: BukkitData? = null

    override val dependencies = mutableListOf<String>()
    override val softDependencies = mutableListOf<String>()

    fun hasPrefix() = data?.prefix?.isNotBlank() == true

    fun hasLoadBefore() = listContainsAtLeastOne(data?.loadBefore)
    fun setLoadBefore(string: String) {
        data?.loadBefore?.let { loadBefore ->
            loadBefore.clear()
            loadBefore.addAll(commaSplit(string))
        }
    }

    override fun hasDependencies() = listContainsAtLeastOne(dependencies)
    override fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override fun hasSoftDependencies() = listContainsAtLeastOne(softDependencies)
    override fun setSoftDependencies(string: String) {
        softDependencies.clear()
        softDependencies.addAll(commaSplit(string))
    }

    fun hasWebsite() = !base?.website.isNullOrBlank()

    override fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator) {
        if (project.isDisposed) {
            return
        }

        val baseConfig = base ?: return
        val dirs = buildSystem.directories ?: return

        runWriteTask {
            indicator.text = "Writing main class"
            // Create plugin main class
            var file = dirs.sourceDirectory
            val files = baseConfig.mainClass.split(".").toTypedArray()
            val className = files.last()

            val packageName = baseConfig.mainClass.substring(0, baseConfig.mainClass.length - className.length - 1)
            file = getMainClassDirectory(files, file)

            val mainClassFile = file.findOrCreateChildData(this, "$className.java")

            BukkitTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className)
            val pluginYml = dirs.resourceDirectory.findOrCreateChildData(this, "plugin.yml")
            BukkitTemplate.applyPluginDescriptionFileTemplate(project, pluginYml, this, buildSystem)

            // Set the editor focus on the main class
            PsiManager.getInstance(project).findFile(mainClassFile)?.let { mainClassPsi ->
                EditorHelper.openInEditor(mainClassPsi)
            }
        }
    }

    override fun setupDependencies(buildSystem: BuildSystem) {
        val mcVersion = data?.minecraftVersion ?: return
        when (type) {
            PlatformType.PAPER -> {
                buildSystem.repositories.add(BuildRepository(
                    "papermc-repo",
                    "https://papermc.io/repo/repository/maven-public/"
                ))
                buildSystem.dependencies.add(BuildDependency(
                    "com.destroystokyo.paper",
                    "paper-api",
                    "$mcVersion-R0.1-SNAPSHOT",
                    mavenScope = "provided",
                    gradleConfiguration = "compileOnly"
                ))
                addSonatype(buildSystem.repositories)
            }
            PlatformType.SPIGOT -> {
                spigotRepo(buildSystem.repositories)
                buildSystem.dependencies.add(BuildDependency(
                    "org.spigotmc",
                    "spigot-api",
                    "$mcVersion-R0.1-SNAPSHOT",
                    mavenScope = "provided",
                    gradleConfiguration = "compileOnly"
                ))
                addSonatype(buildSystem.repositories)
            }
            PlatformType.BUKKIT -> {
                spigotRepo(buildSystem.repositories)
                buildSystem.dependencies.add(BuildDependency(
                    "org.bukkit",
                    "bukkit",
                    "$mcVersion-R0.1-SNAPSHOT",
                    mavenScope = "provided",
                    gradleConfiguration = "compileOnly"
                ))
            }
            else -> {}
        }
    }

    private fun spigotRepo(list: MutableList<BuildRepository>) {
        list.add(BuildRepository(
            "spigotmc-repo",
            "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
        ))
    }
}
