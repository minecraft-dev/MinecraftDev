/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.creator.platformtype

import com.demonwav.mcdev.asset.MCDevBundle
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
    override val label
        get() = MCDevBundle("creator.ui.platform.label")

    class TypeFactory : PlatformTypeStep.Factory {
        override val name
            get() = MCDevBundle("creator.ui.platform.mod.name")
        override fun createStep(parent: PlatformTypeStep) = ModPlatformStep(parent)
    }

    interface Factory : NewProjectWizardMultiStepFactory<ModPlatformStep>
}
