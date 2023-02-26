/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractRunGradleTaskStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.GRADLE_VERSION_KEY
import com.demonwav.mcdev.creator.buildsystem.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.addGradleWrapperProperties
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.UseMixinsStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.lang.JavaVersion
import java.util.Locale

private val fg5WrapperVersion = SemanticVersion.release(7, 5, 1)

const val MAGIC_RUN_CONFIGS_FILE = ".hello_from_mcdev"

class ForgeGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> ForgeGradleFilesStep(parent).chain(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> ForgeCompileJavaStep(parent).chain(::GradleImportStep)
            else -> EmptyStep(parent)
        }
    }
}

class ForgeGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    private fun transformModName(modName: String): String {
        return modName.lowercase(Locale.ENGLISH).replace(" ", "")
    }

    override fun setupAssets(project: Project) {
        val mcVersion = data.getUserData(ForgeVersionChainStep.MC_VERSION_KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionChainStep.FORGE_VERSION_KEY) ?: return
        val modName = transformModName(data.getUserData(AbstractModNameStep.KEY) ?: return)
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = context.projectJdk.versionString?.let(JavaVersion::parse)
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false

        data.putUserData(GRADLE_VERSION_KEY, fg5WrapperVersion)

        assets.addTemplateProperties(
            "MOD_NAME" to modName,
            "MCP_CHANNEL" to "official",
            "MCP_VERSION" to mcVersion,
            "MCP_MC_VERSION" to mcVersion,
            "FORGE_VERSION" to "$mcVersion-$forgeVersion",
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_VERSION" to buildSystemProps.version,
            "HAS_DATA" to "true",
        )

        if (javaVersion != null) {
            assets.addTemplateProperties("JAVA_VERSION" to javaVersion.feature)
        }

        if (authors.isNotEmpty()) {
            assets.addTemplateProperties("AUTHOR_LIST" to authors.joinToString(", "))
        }

        if (useMixins) {
            assets.addTemplateProperties("MIXINS" to "true")
        }

        if (forgeVersion >= SemanticVersion.release(39, 0, 88)) {
            assets.addTemplateProperties("GAME_TEST_FRAMEWORK" to "true")
        }

        if (mcVersion <= MinecraftVersions.MC1_16_5) {
            assets.addTemplateProperties(
                "MCP_CHANNEL" to "snapshot",
                "MCP_VERSION" to "20210309",
            )
        }

        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.FG3_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.FG3_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.FG3_SETTINGS_GRADLE_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }

        WriteAction.runAndWait<Throwable> {
            val dir = VfsUtil.createDirectoryIfMissing(
                LocalFileSystem.getInstance(),
                "${assets.outputDirectory}/.gradle"
            )
                ?: throw IllegalStateException("Unable to create .gradle directory")
            val file = dir.findOrCreateChildData(this, MAGIC_RUN_CONFIGS_FILE)
            val fileContents = buildSystemProps.artifactId + "\n" +
                mcVersion + "\n" +
                forgeVersion + "\n" +
                "genIntellijRuns"
            VfsUtil.saveText(file, fileContents)
        }
    }
}

class ForgeCompileJavaStep(parent: NewProjectWizardStep) : AbstractRunGradleTaskStep(parent) {
    override val task = "compileJava"
}
