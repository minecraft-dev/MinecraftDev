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

package com.demonwav.mcdev.platform.hybrid

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.sponge.SpongeTemplate
import com.demonwav.mcdev.platform.sponge.writeMainSpongeClass
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager

class SpongeForgeProjectConfiguration : ForgeProjectConfiguration() {

    override var type: PlatformType = PlatformType.SPONGE

    var spongeApiVersion = ""

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
            SpongeTemplate.applyMainClassTemplate(
                project,
                mainClassFile,
                packageName,
                className,
                hasDependencies()
            )

            writeMcmodInfo(project, baseConfig, buildSystem, dirs)

            val mainClassPsi = PsiManager.getInstance(project).findFile(mainClassFile) as? PsiJavaFile ?: return@runWriteTask
            val psiClass = mainClassPsi.classes[0]

            writeMainSpongeClass(
                project,
                mainClassPsi,
                psiClass,
                buildSystem,
                baseConfig.pluginName,
                baseConfig.description ?: "",
                baseConfig.website ?: "",
                hasAuthors(),
                baseConfig.authors,
                hasDependencies(),
                dependencies
            )

            // Set the editor focus on the main class
            EditorHelper.openInEditor(mainClassPsi)
        }
    }
}
