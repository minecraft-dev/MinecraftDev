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

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.PlatformVersion
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.getVersionSelector
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.step.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.step.AbstractLatentStep
import com.demonwav.mcdev.creator.step.AbstractSelectVersionStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DependStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.PluginIdStep
import com.demonwav.mcdev.creator.step.PluginNameStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.onShown
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.coroutineScope

class VelocityPlatformStep(parent: PluginPlatformStep) : AbstractLatentStep<PlatformVersion>(parent) {
    override val description = "download Velocity versions"

    override suspend fun computeData() = coroutineScope {
        asyncIO { getVersionSelector(PlatformType.VELOCITY) }.await()
    }

    override fun createStep(data: PlatformVersion) =
        VelocityVersionStep(this, data.versions.mapNotNull(SemanticVersion::tryParse))
            .nextStep(::PluginIdStep)
            .nextStep(::PluginNameStep)
            .nextStep(::MainClassStep)
            .nextStep(::VelocityOptionalSettingsStep)
            .nextStep(::VelocityBuildSystemStep)
            .nextStep(::VelocityProjectFilesStep)
            .nextStep(::VelocityPostBuildSystemStep)

    class Factory : PluginPlatformStep.Factory {
        override val name = "Velocity"

        override fun createStep(parent: PluginPlatformStep) = VelocityPlatformStep(parent)
    }
}

class VelocityVersionStep(
    parent: NewProjectWizardStep,
    versions: List<SemanticVersion>,
) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Velocity Version:"

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
                version >= SemanticVersion.release(3) -> JavaSdkVersion.JDK_11
                else -> JavaSdkVersion.JDK_1_8
            }
            findStep<JdkProjectSetupFinalizer>().setPreferredJdk(preferredJdk, "Velocity $version")
        }
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${VelocityVersionStep::class.java.name}.version")
    }
}

class VelocityOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this)
        .nextStep(::AuthorsStep)
        .nextStep(::WebsiteStep)
        .nextStep(::DependStep)
}
