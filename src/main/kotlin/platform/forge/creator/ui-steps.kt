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

package com.demonwav.mcdev.platform.forge.creator

import com.demonwav.mcdev.creator.platformtype.ModPlatformStep
import com.demonwav.mcdev.creator.step.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.step.AbstractLatentStep
import com.demonwav.mcdev.creator.step.AbstractMcVersionChainStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.ForgeStyleModIdStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.ModNameStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.UpdateUrlStep
import com.demonwav.mcdev.creator.step.UseMixinsStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.IncorrectOperationException
import kotlinx.coroutines.coroutineScope

private val minSupportedMcVersion = MinecraftVersions.MC1_16_5

class ForgePlatformStep(parent: ModPlatformStep) : AbstractLatentStep<ForgeVersion>(parent) {
    override val description = "fetch Forge versions"

    override suspend fun computeData() = coroutineScope {
        asyncIO { ForgeVersion.downloadData() }.await()
    }

    override fun createStep(data: ForgeVersion) = ForgeVersionChainStep(this, data)
        .nextStep(::ForgeStyleModIdStep)
        .nextStep(::ModNameStep)
        .nextStep(::MainClassStep)
        .nextStep(::UseMixinsStep)
        .nextStep(::LicenseStep)
        .nextStep(::ForgeOptionalSettingsStep)
        .nextStep(::ForgeBuildSystemStep)
        .nextStep(::ForgeProjectFilesStep)
        .nextStep(::ForgeMixinsJsonStep)
        .nextStep(::ForgePostBuildSystemStep)
        .nextStep(::ForgeReformatPackDescriptorStep)

    class Factory : ModPlatformStep.Factory {
        override val name = "Forge"
        override fun createStep(parent: ModPlatformStep) = ForgePlatformStep(parent)
    }
}

class ForgeVersionChainStep(
    parent: NewProjectWizardStep,
    private val forgeVersionData: ForgeVersion,
) : AbstractMcVersionChainStep(parent, "Forge Version:") {
    companion object {
        private const val FORGE_VERSION = 1

        val MC_VERSION_KEY = Key.create<SemanticVersion>("${ForgeVersionChainStep::class.java}.mcVersion")
        val FORGE_VERSION_KEY = Key.create<SemanticVersion>("${ForgeVersionChainStep::class.java}.forgeVersion")
    }

    override fun getAvailableVersions(versionsAbove: List<Comparable<*>>): List<Comparable<*>> {
        return when (versionsAbove.size) {
            MINECRAFT_VERSION -> forgeVersionData.sortedMcVersions.filter { it >= minSupportedMcVersion }
            FORGE_VERSION -> forgeVersionData.getForgeVersions(versionsAbove[MINECRAFT_VERSION] as SemanticVersion)
            else -> throw IncorrectOperationException()
        }
    }

    override fun setupProject(project: Project) {
        super.setupProject(project)
        data.putUserData(MC_VERSION_KEY, getVersion(MINECRAFT_VERSION) as SemanticVersion)
        data.putUserData(FORGE_VERSION_KEY, getVersion(FORGE_VERSION) as SemanticVersion)
    }
}

class ForgeOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this)
        .nextStep(::AuthorsStep)
        .nextStep(::WebsiteStep)
        .nextStep(::UpdateUrlStep)
}
