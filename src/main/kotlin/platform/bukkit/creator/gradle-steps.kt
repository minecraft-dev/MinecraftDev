/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractPatchGradleFilesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.ReformatBuildGradleStep
import com.demonwav.mcdev.creator.buildsystem.addGradleWrapperProperties
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.SimpleMcVersionStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class BukkitGradleSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> BukkitGradleFilesStep(parent)
                .nextStep(::BukkitPatchBuildGradleStep)
                .nextStep(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent).nextStep(::ReformatBuildGradleStep)
            else -> EmptyStep(parent)
        }
    }
}

class BukkitGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "PLUGIN_VERSION" to buildSystemProps.version,
            "JAVA_VERSION" to javaVersion,
        )
        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.BUKKIT_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.BUKKIT_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.BUKKIT_SETTINGS_GRADLE_TEMPLATE,
        )
        assets.addGradleWrapperProperties(project)

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }
    }
}

class BukkitPatchBuildGradleStep(parent: NewProjectWizardStep) : AbstractPatchGradleFilesStep(parent) {
    override fun patch(project: Project, gradleFiles: GradleFiles) {
        val platform = data.getUserData(AbstractBukkitPlatformStep.KEY) ?: return
        val mcVersion = data.getUserData(SimpleMcVersionStep.KEY) ?: return
        val repositories = platform.getRepositories(mcVersion)
        val dependencies = platform.getDependencies(mcVersion)
        addRepositories(project, gradleFiles.buildGradle, repositories)
        addDependencies(project, gradleFiles.buildGradle, dependencies)
    }
}
