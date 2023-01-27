/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

class SimpleMcVersionStep(
    parent: NewProjectWizardStep,
    versions: List<SemanticVersion>
) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Minecraft Version:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${SimpleMcVersionStep::class.java.name}.version")
    }
}
