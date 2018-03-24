/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

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

    var generateDocumentation = false
    var spongeApiVersion = ""

    init {
        // We set our platform type to sponge because we want it to provide us the dependency. The GradleBuildSystem
        // will properly handle us as a a combined project
        type = PlatformType.SPONGE
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
            SpongeTemplate.applyMainClassTemplate(
                project,
                mainClassFile,
                packageName,
                className,
                hasDependencies()
            )

            writeMcmodInfo(project, buildSystem)

            val mainClassPsi = PsiManager.getInstance(project).findFile(mainClassFile) as? PsiJavaFile ?: return@runWriteTask
            val psiClass = mainClassPsi.classes[0]

            writeMainSpongeClass(
                project,
                mainClassPsi,
                psiClass,
                buildSystem,
                pluginName,
                description,
                website ?: "",
                hasAuthors(),
                authors,
                hasDependencies(),
                dependencies
            )

            // Set the editor focus on the main class
            EditorHelper.openInEditor(mainClassPsi)
        }
    }
}
