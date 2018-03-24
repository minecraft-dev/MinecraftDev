/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

open class ForgeProjectConfiguration : ProjectConfiguration() {

    val dependencies = mutableListOf<String>()
    var updateUrl: String = ""

    var mcpVersion: String = ""
    var forgeVersion: String = ""
    var mcVersion: String = ""

    init {
        type = PlatformType.FORGE
    }

    fun hasDependencies() = listContainsAtLeastOne(dependencies)
    fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator) {
        runWriteTask {
            indicator.text = "Writing main class"
            var file = buildSystem.sourceDirectory
            val files = mainClass.split(".").toTypedArray()
            val className = files.last()
            val packageName = mainClass.substring(0, mainClass.length - className.length - 1)
            file = getMainClassDirectory(files, file)

            val mainClassFile = file.findOrCreateChildData(this, className + ".java")
            ForgeTemplate.applyMainClassTemplate(
                project,
                mainClassFile,
                packageName,
                buildSystem.artifactId,
                pluginName,
                pluginVersion,
                className
            )

            writeMcmodInfo(project, buildSystem)

            // Set the editor focus on the main class
            PsiManager.getInstance(project).findFile(mainClassFile)?.let { mainClassPsi ->
                EditorHelper.openInEditor(mainClassPsi)
            }
        }
    }

    protected fun writeMcmodInfo(project: Project, buildSystem: BuildSystem) {
        val file = buildSystem.resourceDirectory
        val mcmodInfoFile = file.findOrCreateChildData(this, ForgeConstants.MCMOD_INFO)

        val authorsText = authors.joinToString(", ") { "\"$it\"" }
        val dependenciesText = dependencies.joinToString(", ") { "\"$it\"" }

        ForgeTemplate.applyMcmodInfoTemplate(
            project,
            mcmodInfoFile,
            buildSystem.artifactId,
            pluginName,
            description,
            website ?: "",
            updateUrl,
            authorsText,
            dependenciesText
        )
    }
}
