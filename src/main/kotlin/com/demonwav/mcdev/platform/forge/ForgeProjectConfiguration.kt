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

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.mcp.McpVersionPair
import com.demonwav.mcdev.update.PluginUtil.pluginVersion
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

open class ForgeProjectConfiguration : ProjectConfiguration() {

    var updateUrl: String? = null

    override var type: PlatformType = PlatformType.FORGE

    var mcpVersion = McpVersionPair("", "")
    var forgeVersion: String = ""
    var mcVersion: String = ""

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

            val mainClassFile = file.findOrCreateChildData(this, "$className.java")
            ForgeTemplate.applyMainClassTemplate(
                project,
                mainClassFile,
                packageName,
                buildSystem.artifactId,
                baseConfig.pluginName,
                pluginVersion,
                className,
                SemanticVersion.parse(mcVersion)
            )

            writeDescriptor(project, baseConfig, buildSystem, dirs)

            // Set the editor focus on the main class
            PsiManager.getInstance(project).findFile(mainClassFile)?.let { mainClassPsi ->
                EditorHelper.openInEditor(mainClassPsi)
            }
        }
    }

    private fun writeDescriptor(
        project: Project,
        baseConfigs: BaseConfigs,
        buildSystem: BuildSystem,
        dirs: BuildSystem.DirectorySet
    ) {
        val file = dirs.resourceDirectory
        val mcVer = SemanticVersion.parse(mcVersion)
        val descriptorFile = if (mcVer < ForgeModuleType.FG3_VERSION) {
            file.findOrCreateChildData(this, ForgeConstants.MCMOD_INFO)
        } else {
            var meta = file.findChild("META-INF")
            if (meta == null) {
                meta = file.createChildDirectory(this, ForgeConstants.META_INF)
            }
            meta.findOrCreateChildData(this, ForgeConstants.MODS_TOML)
        }

        val authorsText = baseConfigs.authors.joinToString(", ") { "\"$it\"" }

        ForgeTemplate.applyModDescriptorTemplate(
            project,
            descriptorFile,
            buildSystem.artifactId,
            baseConfigs.pluginName,
            baseConfigs.description ?: "",
            baseConfigs.website,
            updateUrl,
            authorsText,
            mcVer
        )

        if (mcVer >= ForgeModuleType.FG3_VERSION) {
            val packFile = dirs.resourceDirectory.findOrCreateChildData(this, ForgeConstants.PACK_MCMETA)
            ForgeTemplate.applyPackMcmetaTemplate(project, packFile, buildSystem.artifactId)
        }
    }

    override fun setupDependencies(buildSystem: BuildSystem) {}
}
