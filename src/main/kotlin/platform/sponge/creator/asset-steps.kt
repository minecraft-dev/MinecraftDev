/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
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
        val (packageName, className) = splitPackage(data.getUserData(MainClassStep.KEY) ?: return)

        assets.addTemplateProperties(
            "PLUGIN_ID" to buildSystemProps.artifactId,
            "PACKAGE" to packageName,
            "CLASS_NAME" to className,
        )
        val mainClassFile = "src/main/java/${packageName.replace('.', '/')}/$className.java"
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
