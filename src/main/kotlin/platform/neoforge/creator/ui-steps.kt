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

package com.demonwav.mcdev.platform.neoforge.creator

import com.demonwav.mcdev.creator.ParchmentStep
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
import com.demonwav.mcdev.platform.neoforge.version.NeoForgeVersion
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.IncorrectOperationException
import kotlinx.coroutines.coroutineScope

private val minSupportedMcVersion = MinecraftVersions.MC1_20_2

class NeoForgePlatformStep(parent: ModPlatformStep) : AbstractLatentStep<NeoForgeVersion>(parent) {
    override val description = "fetch NeoForge versions"

    override suspend fun computeData() = coroutineScope {
        asyncIO { NeoForgeVersion.downloadData() }.await()
    }

    override fun createStep(data: NeoForgeVersion) = NeoForgeVersionChainStep(this, data)
        .nextStep(::ForgeStyleModIdStep)
        .nextStep(::ModNameStep)
        .nextStep(::MainClassStep)
        .nextStep(::UseMixinsStep)
        .nextStep(::LicenseStep)
        .nextStep(::NeoForgeOptionalSettingsStep)
        .nextStep(::NeoForgeBuildSystemStep)
        .nextStep(::NeoForgeProjectFilesStep)
        .nextStep(::NeoForgeMixinsJsonStep)
        .nextStep(::NeoForgePostBuildSystemStep)
        .nextStep(::NeoForgeReformatPackDescriptorStep)

    class Factory : ModPlatformStep.Factory {
        override val name = "NeoForge"
        override fun createStep(parent: ModPlatformStep) = NeoForgePlatformStep(parent)
    }
}

class NeoForgeVersionChainStep(
    parent: NewProjectWizardStep,
    private val neoforgeVersionData: NeoForgeVersion,
) : AbstractMcVersionChainStep(parent, "NeoForge Version:") {
    companion object {
        private const val NEOFORGE_VERSION = 1

        val MC_VERSION_KEY = Key.create<SemanticVersion>("${NeoForgeVersionChainStep::class.java}.mcVersion")
        val NEOFORGE_VERSION_KEY =
            Key.create<SemanticVersion>("${NeoForgeVersionChainStep::class.java}.neoforgeVersion")
    }

    override fun getAvailableVersions(versionsAbove: List<Comparable<*>>): List<Comparable<*>> {
        return when (versionsAbove.size) {
            MINECRAFT_VERSION -> neoforgeVersionData.sortedMcVersions.filter { it >= minSupportedMcVersion }
            NEOFORGE_VERSION ->
                neoforgeVersionData.getForgeVersions(versionsAbove[MINECRAFT_VERSION] as SemanticVersion)
            else -> throw IncorrectOperationException()
        }
    }

    override fun setupProject(project: Project) {
        super.setupProject(project)
        data.putUserData(MC_VERSION_KEY, getVersion(MINECRAFT_VERSION) as SemanticVersion)
        data.putUserData(NEOFORGE_VERSION_KEY, getVersion(NEOFORGE_VERSION) as SemanticVersion)
    }
}

class NeoForgeOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this)
        .nextStep(::AuthorsStep)
        .nextStep(::WebsiteStep)
        .nextStep(::UpdateUrlStep)
        .nextStep(::ParchmentStep)
}
