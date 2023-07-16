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

package com.demonwav.mcdev.platform.architectury.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.GRADLE_VERSION_KEY
import com.demonwav.mcdev.creator.buildsystem.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.addGradleWrapperProperties
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModIdStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

private val NewProjectWizardStep.architecturyGroup: String get() {
    val apiVersion = data.getUserData(ArchitecturyVersionChainStep.ARCHITECTURY_API_VERSION_KEY)
    return when {
        apiVersion == null || apiVersion >= SemanticVersion.release(2, 0, 10) -> "dev.architectury"
        else -> "me.shedaniel"
    }
}

class ArchitecturyGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> ArchitecturyGradleFilesStep(parent).nextStep(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent)
            else -> EmptyStep(parent)
        }
    }
}

class ArchitecturyGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        data.putUserData(GRADLE_VERSION_KEY, SemanticVersion.release(7, 6, 1))

        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modId = data.getUserData(AbstractModIdStep.KEY) ?: return
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mcVersion = data.getUserData(ArchitecturyVersionChainStep.MC_VERSION_KEY) ?: return
        val forgeVersion = data.getUserData(ArchitecturyVersionChainStep.FORGE_VERSION_KEY) ?: return
        val fabricLoaderVersion = data.getUserData(ArchitecturyVersionChainStep.FABRIC_LOADER_VERSION_KEY) ?: return
        val fabricApiVersion = data.getUserData(ArchitecturyVersionChainStep.FABRIC_API_VERSION_KEY)
        val archApiVersion = data.getUserData(ArchitecturyVersionChainStep.ARCHITECTURY_API_VERSION_KEY)
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_ID" to modId,
            "MOD_NAME" to modName,
            "VERSION" to buildSystemProps.version,
            "MC_VERSION" to mcVersion,
            "FORGE_VERSION" to "$mcVersion-$forgeVersion",
            "FABRIC_LOADER_VERSION" to fabricLoaderVersion,
            "ARCHITECTURY_GROUP" to architecturyGroup,
            "JAVA_VERSION" to javaVersion,
        )

        if (fabricApiVersion != null) {
            assets.addTemplateProperties(
                "FABRIC_API_VERSION" to fabricApiVersion,
                "FABRIC_API" to "true",
            )
        }

        if (archApiVersion != null) {
            assets.addTemplateProperties(
                "ARCHITECTURY_API_VERSION" to archApiVersion,
                "ARCHITECTURY_API" to "true",
            )
        }

        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.ARCHITECTURY_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.ARCHITECTURY_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.ARCHITECTURY_SETTINGS_GRADLE_TEMPLATE,
            "common/build.gradle" to MinecraftTemplates.ARCHITECTURY_COMMON_BUILD_GRADLE_TEMPLATE,
            "forge/build.gradle" to MinecraftTemplates.ARCHITECTURY_FORGE_BUILD_GRADLE_TEMPLATE,
            "forge/gradle.properties" to MinecraftTemplates.ARCHITECTURY_FORGE_GRADLE_PROPERTIES_TEMPLATE,
            "fabric/build.gradle" to MinecraftTemplates.ARCHITECTURY_FABRIC_BUILD_GRADLE_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }
    }
}
