/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.step

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

abstract class AbstractLongRunningAssetsStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    protected val assets = object : FixedAssetsNewProjectWizardStep(parent) {
        override fun setupAssets(project: Project) {
            outputDirectory = context.projectFileDirectory
            this@AbstractLongRunningAssetsStep.setupAssets(project)
        }
    }

    abstract fun setupAssets(project: Project)

    override fun perform(project: Project) {
        assets.setupProject(project)
    }
}
