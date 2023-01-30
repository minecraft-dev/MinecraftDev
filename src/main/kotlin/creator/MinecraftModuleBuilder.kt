/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.platformtype.PlatformTypeStep
import com.demonwav.mcdev.platform.MinecraftModuleType
import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractNewProjectWizardBuilder
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.roots.ModifiableRootModel

class MinecraftModuleBuilder : AbstractNewProjectWizardBuilder() {

    override fun getPresentableName() = MinecraftModuleType.NAME
    override fun getNodeIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getGroupName() = MinecraftModuleType.NAME
    override fun getBuilderId() = "MINECRAFT_MODULE"
    override fun getDescription() = "Create a new Minecraft project"

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        if (moduleJdk != null) {
            modifiableRootModel.sdk = moduleJdk
        } else {
            modifiableRootModel.inheritSdk()
        }
    }

    override fun getParentGroup() = MinecraftModuleType.NAME
    override fun createStep(context: WizardContext) = RootNewProjectWizardStep(context).chain(
        ::NewProjectWizardBaseStep,
        ::PlatformTypeStep,
        ::BuildSystemPropertiesStep,
        ::ProjectSetupFinalizerWizardStep,
    )

    override fun getIgnoredSteps() = listOf(ProjectSettingsStep::class.java)
}
