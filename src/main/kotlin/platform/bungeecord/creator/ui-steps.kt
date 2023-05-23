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

package com.demonwav.mcdev.platform.bungeecord.creator

import com.demonwav.mcdev.creator.PlatformVersion
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.getVersionSelector
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.step.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.step.AbstractLatentStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DependStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.PluginNameStep
import com.demonwav.mcdev.creator.step.SimpleMcVersionStep
import com.demonwav.mcdev.creator.step.SoftDependStep
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStep
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.coroutineScope

class BungeePlatformStep(
    parent: PluginPlatformStep,
) : AbstractNewProjectWizardMultiStep<BungeePlatformStep, BungeePlatformStep.Factory>(parent, EP_NAME) {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.bungeePlatformWizard")
    }

    override val self = this
    override val label = "Bungee Platform:"

    class PlatformFactory : PluginPlatformStep.Factory {
        override val name = "BungeeCord"

        override fun createStep(parent: PluginPlatformStep) = BungeePlatformStep(parent)
    }

    interface Factory : NewProjectWizardMultiStepFactory<BungeePlatformStep>
}

abstract class AbstractBungeePlatformStep(
    parent: BungeePlatformStep,
    private val platform: PlatformType,
) : AbstractLatentStep<PlatformVersion>(parent) {
    override val description = "download versions"

    override suspend fun computeData() = coroutineScope {
        try {
            asyncIO { getVersionSelector(platform) }.await()
        } catch (e: Throwable) {
            null
        }
    }

    override fun createStep(data: PlatformVersion) =
        SimpleMcVersionStep(this, data.versions.mapNotNull(SemanticVersion::tryParse))
            .nextStep(::PluginNameStep)
            .nextStep(::MainClassStep)
            .nextStep(::BungeeOptionalSettingsStep)
            .nextStep(::BungeeBuildSystemStep)
            .nextStep(::BungeeProjectFilesStep)
            .nextStep(::BungeePostBuildSystemStep)

    override fun setupProject(project: Project) {
        data.putUserData(KEY, this)
        super.setupProject(project)
    }

    abstract fun getRepositories(mcVersion: SemanticVersion): List<BuildRepository>

    abstract fun getDependencies(mcVersion: SemanticVersion): List<BuildDependency>

    companion object {
        val KEY = Key.create<AbstractBungeePlatformStep>("${AbstractBungeePlatformStep::class.java.name}.platform")
    }
}

class BungeeOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this)
        .nextStep(::AuthorsStep)
        .nextStep(::DependStep)
        .nextStep(::SoftDependStep)
}
