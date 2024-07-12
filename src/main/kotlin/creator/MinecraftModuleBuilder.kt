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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.platformtype.PlatformTypeStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractNewProjectWizardBuilder
import com.intellij.ide.wizard.GitNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.openapi.roots.ModifiableRootModel

class MinecraftModuleBuilder : AbstractNewProjectWizardBuilder() {

    override fun getPresentableName() = "Minecraft (Old Wizard)"
    override fun getNodeIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getGroupName() = "Minecraft"
    override fun getBuilderId() = "MINECRAFT_MODULE"
    override fun getDescription() = MCDevBundle("creator.ui.create_minecraft_project")

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        if (moduleJdk != null) {
            modifiableRootModel.sdk = moduleJdk
        } else {
            modifiableRootModel.inheritSdk()
        }
    }

    override fun getParentGroup() = "Minecraft"

    override fun createStep(context: WizardContext) = RootNewProjectWizardStep(context)
        .nextStep(::NewProjectWizardBaseStep)
        .nextStep(::GitNewProjectWizardStep)
        .nextStep(PlatformTypeStep::create)
        .nextStep(::BuildSystemPropertiesStep)
        .nextStep(::ProjectSetupFinalizerWizardStep)

    override fun getIgnoredSteps() = listOf(ProjectSettingsStep::class.java)
}
