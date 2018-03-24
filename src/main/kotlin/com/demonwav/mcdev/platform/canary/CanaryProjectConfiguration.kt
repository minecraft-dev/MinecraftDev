/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

class CanaryProjectConfiguration : ProjectConfiguration() {

    val dependencies = mutableListOf<String>()
    var isEnableEarly = false
    var canaryVersion = ""

    fun hasDependencies() = listContainsAtLeastOne(dependencies)
    fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator) {
        runWriteTask {
            indicator.text = "Writing main class"
            // Create plugin main class
            var file = buildSystem.sourceDirectory
            val files = mainClass.split(".").toTypedArray()
            val className = files.last()

            val packageName = mainClass.substring(0, mainClass.length - className.length - 1)
            file = getMainClassDirectory(files, file)

            val mainClassFile = file.findOrCreateChildData(this, className + ".java")

            CanaryTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className)
            val canaryInf = buildSystem.resourceDirectory.findOrCreateChildData(this, "Canary.inf")
            CanaryTemplate.applyPluginDescriptionFileTemplate(project, canaryInf, this, buildSystem)

            // Set the editor focus on the main class
            PsiManager.getInstance(project).findFile(mainClassFile)?.let { mainClassPsi ->
                EditorHelper.openInEditor(mainClassPsi)
            }
        }
    }
}
