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

import com.demonwav.mcdev.creator.platformtype.ModPlatformStep.Factory
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStep
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * The step to select a mod platform.
 *
 * To add custom mod platforms, register a [Factory] to the `com.demonwav.minecraft-dev.modPlatformWizard` extension
 * point.
 */
class ModPlatformStep(
    parent: PlatformTypeStep,
) : AbstractNewProjectWizardMultiStep<ModPlatformStep, Factory>(parent, EP_NAME) {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.modPlatformWizard")
    }

    override val self = this
    override val label = "Platform:"

    class TypeFactory : PlatformTypeStep.Factory {
        override val name = "Mod"
        override fun createStep(parent: PlatformTypeStep) = ModPlatformStep(parent)
    }

    interface Factory : NewProjectWizardMultiStepFactory<ModPlatformStep>
}
