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

package com.demonwav.mcdev.platform.bungeecord

import com.demonwav.mcdev.buildsystem.BuildDependency
import com.demonwav.mcdev.buildsystem.BuildRepository
import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.bukkit.BukkitLikeConfiguration
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

class BungeeCordProjectConfiguration(override var type: PlatformType) : ProjectConfiguration(),
    BukkitLikeConfiguration {

    override val dependencies = mutableListOf<String>()
    override val softDependencies = mutableListOf<String>()
    var minecraftVersion = ""

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

            BungeeCordTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className)
            val pluginYml = dirs.resourceDirectory.findOrCreateChildData(this, "plugin.yml")
            BungeeCordTemplate.applyPluginDescriptionFileTemplate(project, pluginYml, this, buildSystem)

            // Set the editor focus on the main class
            PsiManager.getInstance(project).findFile(mainClassFile)?.let { mainClassPsi ->
                EditorHelper.openInEditor(mainClassPsi)
            }
        }
    }

    override fun setupDependencies(buildSystem: BuildSystem) {
        addSonatype(buildSystem.repositories)
        when (type) {
            PlatformType.WATERFALL -> {
                buildSystem.repositories.add(BuildRepository(
                    "destroystokyo-repo",
                    "https://repo.destroystokyo.com/repository/maven-public/"
                ))
                buildSystem.dependencies.add(BuildDependency(
                    "io.github.waterfallmc",
                    "waterfall-api",
                    "$minecraftVersion-SNAPSHOT",
                    mavenScope = "provided",
                    gradleConfiguration = "compileOnly"
                ))
            }
            PlatformType.BUNGEECORD -> {
                buildSystem.dependencies.add(BuildDependency(
                    "net.md-5",
                    "bungeecord-api",
                    "$minecraftVersion-SNAPSHOT",
                    mavenScope = "provided",
                    gradleConfiguration = "compileOnly"
                ))
            }
            else -> {}
        }
    }
}
