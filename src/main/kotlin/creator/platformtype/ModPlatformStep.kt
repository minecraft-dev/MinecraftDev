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
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.openapi.extensions.ExtensionPointName

class ModPlatformStep(parent: PlatformTypeStep) : AbstractNewProjectWizardMultiStep<ModPlatformStep, ModPlatformStep.Factory>(parent, EP_NAME) {
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