/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.platformtype

import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.openapi.extensions.ExtensionPointName

class PlatformTypeStep(
    parent: NewProjectWizardBaseStep
) : AbstractNewProjectWizardMultiStep<PlatformTypeStep, PlatformTypeStep.Factory>(parent, EP_NAME),
    NewProjectWizardBaseData by parent {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.platformTypeWizard")
    }

    override val self = this
    override val label = "Platform Type:"

    interface Factory : NewProjectWizardMultiStepFactory<PlatformTypeStep>
}
