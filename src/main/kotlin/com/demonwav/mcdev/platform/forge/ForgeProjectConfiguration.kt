/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

@file:Suppress("Duplicates")

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.mixin.reference.InjectionPointType.description
import com.demonwav.mcdev.update.PluginUtil.pluginVersion
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

open class ForgeProjectConfiguration : ProjectConfiguration() {

    val dependencies = mutableListOf<String>()
    var updateUrl: String = ""

    override var type: PlatformType = PlatformType.FORGE

    var mcpVersion: String = ""
    var forgeVersion: String = ""
    var mcVersion: String = ""

    fun hasDependencies() = listContainsAtLeastOne(dependencies)
    fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator) {
        if (project.isDisposed) {
            return
        }

        val baseConfig = base ?: return
        val dirs = buildSystem.directories ?: return

        runWriteTask {
            indicator.text = "Writing main class"
            var file = dirs.sourceDirectory
            val files = baseConfig.mainClass.split(".").toTypedArray()
            val className = files.last()
            val packageName = baseConfig.mainClass.substring(0, baseConfig.mainClass.length - className.length - 1)
            file = getMainClassDirectory(files, file)

            val mainClassFile = file.findOrCreateChildData(this, className + ".java")
            ForgeTemplate.applyMainClassTemplate(
                project,
                mainClassFile,
                packageName,
                buildSystem.artifactId,
                baseConfig.pluginName,
                pluginVersion,
                className
            )

            writeMcmodInfo(project, baseConfig, buildSystem, dirs)

            // Set the editor focus on the main class
            PsiManager.getInstance(project).findFile(mainClassFile)?.let { mainClassPsi ->
                EditorHelper.openInEditor(mainClassPsi)
            }
        }
    }

    protected fun writeMcmodInfo(
        project: Project,
        baseConfigs: BaseConfigs,
        buildSystem: BuildSystem,
        dirs: BuildSystem.DirectorySet
    ) {
        val file = dirs.resourceDirectory
        val mcmodInfoFile = file.findOrCreateChildData(this, ForgeConstants.MCMOD_INFO)

        val authorsText = baseConfigs.authors.joinToString(", ") { "\"$it\"" }
        val dependenciesText = dependencies.joinToString(", ") { "\"$it\"" }

        ForgeTemplate.applyMcmodInfoTemplate(
            project,
            mcmodInfoFile,
            buildSystem.artifactId,
            baseConfigs.pluginName,
            description,
            baseConfigs.website ?: "",
            updateUrl,
            authorsText,
            dependenciesText
        )
    }
}
