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
    override val label
        get() = MCDevBundle("creator.ui.platform.type.label")

    interface Factory : NewProjectWizardMultiStepFactory<PlatformTypeStep>
}
