/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.platformtype

import com.demonwav.mcdev.creator.platformtype.PlatformTypeStep.Factory
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * The step to select the platform type (mod/plugin).
 *
 * To add custom platform types, register a [Factory] to the `com.demonwav.minecraft-dev.platformTypeWizard` extension
 * point.
 */
class PlatformTypeStep private constructor(
    parent: NewProjectWizardStep,
) : AbstractNewProjectWizardMultiStep<PlatformTypeStep, Factory>(parent, EP_NAME),
    NewProjectWizardBaseData by parent as NewProjectWizardBaseData {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.platformTypeWizard")

        fun <P> create(parent: P) where P : NewProjectWizardStep, P : NewProjectWizardBaseData =
            PlatformTypeStep(parent)
    }

    override val self = this
    override val label = "Platform Type:"

    interface Factory : NewProjectWizardMultiStepFactory<PlatformTypeStep>
}
