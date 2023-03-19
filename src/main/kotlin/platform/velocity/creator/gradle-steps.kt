/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractPatchGradleFilesStep
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.GradlePlugin
import com.demonwav.mcdev.creator.buildsystem.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.ReformatBuildGradleStep
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class VelocityGradleSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> VelocityGradleFilesStep(parent)
                .nextStep(::VelocityPatchGradleFilesStep)
                .nextStep(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent)
                .nextStep(::ReformatBuildGradleStep)
                .nextStep { VelocityModifyMainClassStep(it, true) }
            else -> EmptyStep(parent)
        }
    }
}

class VelocityGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val projectName = baseData!!.name
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val (mainPackage, _) = splitPackage(mainClass)

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "PLUGIN_ID" to buildSystemProps.artifactId,
            "PLUGIN_VERSION" to buildSystemProps.version,
            "JAVA_VERSION" to javaVersion,
            "PROJECT_NAME" to projectName,
            "PACKAGE" to mainPackage,
        )

        val buildConstantsJava = "src/main/java/${mainPackage.replace('.', '/')}/BuildConstants.java"
        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.VELOCITY_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.VELOCITY_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.VELOCITY_SETTINGS_GRADLE_TEMPLATE,
            buildConstantsJava to MinecraftTemplates.VELOCITY_BUILD_CONSTANTS_TEMPLATE,
        )

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }
    }
}

class VelocityPatchGradleFilesStep(parent: NewProjectWizardStep) : AbstractPatchGradleFilesStep(parent) {
    override fun patch(project: Project, gradleFiles: GradleFiles) {
        val velocityApiVersion = data.getUserData(VelocityVersionStep.KEY) ?: return

        addPlugins(
            project,
            gradleFiles.buildGradle,
            listOf(
                GradlePlugin("org.jetbrains.gradle.plugin.idea-ext", "1.0.1"),
            ),
        )
        addRepositories(
            project,
            gradleFiles.buildGradle,
            listOf(
                BuildRepository(
                    "papermc-repo",
                    "https://repo.papermc.io/repository/maven-public/",
                ),
            ),
        )
        val annotationArtifactId =
            if (velocityApiVersion >= VelocityConstants.API_4) "velocity-annotation-processor" else "velocity-api"
        addDependencies(
            project,
            gradleFiles.buildGradle,
            listOf(
                BuildDependency(
                    "com.velocitypowered",
                    "velocity-api",
                    velocityApiVersion.toString(),
                    gradleConfiguration = "compileOnly",
                ),
                BuildDependency(
                    "com.velocitypowered",
                    annotationArtifactId,
                    velocityApiVersion.toString(),
                    gradleConfiguration = "annotationProcessor",
                ),
            ),
        )
    }
}
