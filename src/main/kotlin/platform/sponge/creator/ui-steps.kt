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

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.step.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.step.AbstractLatentStep
import com.demonwav.mcdev.creator.step.AbstractSelectVersionStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DependStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.PluginNameStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.sponge.SpongeVersion
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.onShown
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel

private val MIN_SPONGE_VERSION = SpongeConstants.API8

class SpongePlatformStep(parent: PluginPlatformStep) : AbstractLatentStep<SpongeVersion>(parent) {
    override val description = "download Sponge versions"

    override suspend fun computeData() = SpongeVersion.downloadData()

    override fun createStep(data: SpongeVersion) = SpongeApiVersionStep(this, data)
        .nextStep(::PluginNameStep)
        .nextStep(::MainClassStep)
        .nextStep(::LicenseStep)
        .nextStep(::SpongeOptionalSettingsStep)
        .nextStep(::SpongeBuildSystemStep)
        .nextStep(::SpongeProjectFilesStep)
        .nextStep(::SpongePostBuildSystemStep)

    class Factory : PluginPlatformStep.Factory {
        override val name = "Sponge"

        override fun createStep(parent: PluginPlatformStep) = SpongePlatformStep(parent)
    }
}

class SpongeApiVersionStep(
    parent: NewProjectWizardStep,
    data: SpongeVersion,
) : AbstractSelectVersionStep<SemanticVersion>(
    parent,
    data.versions.keys.mapNotNull(SemanticVersion::tryParse).filter { it >= MIN_SPONGE_VERSION },
) {
    override val label = "Sponge API Version:"

    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        versionProperty.afterChange {
            applyJdkVersion()
        }
        versionBox.onShown {
            applyJdkVersion()
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
        applyJdkVersion()
    }

    private fun applyJdkVersion() {
        SemanticVersion.tryParse(version)?.let { version ->
            val preferredJdk = when {
                version >= SpongeConstants.API9 -> JavaSdkVersion.JDK_17
                else -> JavaSdkVersion.JDK_1_8
            }
            findStep<JdkProjectSetupFinalizer>().setPreferredJdk(preferredJdk, "Sponge $version")
        }
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${SpongeApiVersionStep::class.java.name}.version")
    }
}

class SpongeOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this)
        .nextStep(::AuthorsStep)
        .nextStep(::WebsiteStep)
        .nextStep(::DependStep)
}
