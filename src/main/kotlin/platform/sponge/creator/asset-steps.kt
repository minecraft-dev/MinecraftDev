/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class SpongeProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating project files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val (packageName, className) = splitPackage(mainClass)

        assets.addTemplateProperties(
            "PLUGIN_ID" to buildSystemProps.artifactId,
            "PACKAGE" to packageName,
            "CLASS_NAME" to className,
        )
        val mainClassFile = "src/main/java/${mainClass.replace('.', '/')}.java"
        assets.addTemplates(
            project,
            mainClassFile to MinecraftTemplates.SPONGE8_MAIN_CLASS_TEMPLATE,
        )
        assets.addLicense(project)
    }
}

class SpongeBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Sponge"
}

class SpongePostBuildSystemStep(
    parent: NewProjectWizardStep,
) : AbstractRunBuildSystemStep(parent, SpongeBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}
