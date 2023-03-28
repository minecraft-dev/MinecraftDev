/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.quilt.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.*
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class QuiltGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> QuiltGradleFilesStep(parent) // TODO: Read fixed version of Gradle Wrapper Step
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent)
            else -> EmptyStep(parent)
        }
    }
}

class QuiltGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val mcVersion = data.getUserData(QuiltVersionChainStep.MC_VERSION_KEY) ?: return
        val quiltMappingsVersion = data.getUserData(QuiltVersionChainStep.QUILT_MAPPINGS_VERSION_KEY) ?: return
        val loaderVersion = data.getUserData(QuiltVersionChainStep.LOADER_VERSION_KEY) ?: return
        val loomVersion = "1.1.+" // TODO
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val apiVersion = data.getUserData(QuiltVersionChainStep.API_VERSION_KEY)
        val officialMappings = data.getUserData(QuiltVersionChainStep.OFFICIAL_MAPPINGS_KEY) ?: false

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "VERSION" to buildSystemProps.version,
            "MC_VERSION" to mcVersion,
            "QUILT_MAPPINGS" to quiltMappingsVersion,
            "LOADER_VERSION" to loaderVersion,
            "LOOM_VERSION" to loomVersion,
            "JAVA_VERSION" to javaVersion,
        )

        if (apiVersion != null) {
            assets.addTemplateProperties("API_VERSION" to apiVersion)
        }

        if (officialMappings) {
            assets.addTemplateProperties("OFFICIAL_MAPPINGS" to "true")
        }

        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.QUILT_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.QUILT_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.QUILT_SETTINGS_GRADLE_TEMPLATE,
            "gradle/libs.versions.toml" to MinecraftTemplates.QUILT_LIBS_VERSIONS_TEMPLATE
        )

        assets.data.putUserData(GRADLE_VERSION_KEY, SemanticVersion.release(8,0,2))
        assets.addGradleWrapperProperties(project)

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }
    }
}
