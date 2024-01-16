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

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.ReformatBuildGradleStep
import com.demonwav.mcdev.creator.buildsystem.addGradleWrapperProperties
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModIdStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DependStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.starters.local.GeneratorEmptyDirectory
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class SpongeGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> SpongeGradleFilesStep(parent).nextStep(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> SpongeGradleImportStep(parent).nextStep(::ReformatBuildGradleStep)
            else -> EmptyStep(parent)
        }
    }
}

class SpongeGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val spongeVersion = data.getUserData(SpongeApiVersionStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val pluginId = data.getUserData(AbstractModIdStep.KEY) ?: return
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val dependencies = data.getUserData(DependStep.KEY) ?: emptyList()
        val baseData = data.getUserData(NewProjectWizardBaseData.KEY) ?: return

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "PLUGIN_ID" to pluginId,
            "PLUGIN_VERSION" to buildSystemProps.version,
            "JAVA_VERSION" to javaVersion,
            "SPONGEAPI_VERSION" to spongeVersion,
            "LICENSE" to license.id,
            "PLUGIN_NAME" to pluginName,
            "MAIN_CLASS" to mainClass,
            "AUTHORS" to authors,
            "DEPENDENCIES" to dependencies,
            "PROJECT_NAME" to baseData.name,
        )

        if (description.isNotBlank()) {
            assets.addTemplateProperties("DESCRIPTION" to description)
        }

        if (website.isNotBlank()) {
            assets.addTemplateProperties("WEBSITE" to website)
        }

        assets.addTemplates(
            project,
            "build.gradle.kts" to MinecraftTemplates.SPONGE8_BUILD_GRADLE_TEMPLATE,
            "settings.gradle.kts" to MinecraftTemplates.SPONGE8_SETTINGS_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.SPONGE8_GRADLE_PROPERTIES_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)

        assets.addAssets(
            GeneratorEmptyDirectory("src/main/java"),
            GeneratorEmptyDirectory("src/main/resources"),
        )

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }
    }
}

class SpongeGradleImportStep(parent: NewProjectWizardStep) : GradleImportStep(parent) {
    override val additionalRunTasks = listOf("runServer")
}
