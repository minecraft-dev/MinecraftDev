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

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.ParchmentStep
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
import com.demonwav.mcdev.creator.step.AbstractModIdStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.lang.JavaVersion

private val ngWrapperVersion = SemanticVersion.release(8, 4)

const val MAGIC_RUN_CONFIGS_FILE = ".hello_from_mcdev"

class NeoForgeGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> NeoForgeGradleFilesStep(parent).nextStep(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> NeoForgeCompileJavaStep(parent).nextStep(::GradleImportStep)
            else -> EmptyStep(parent)
        }
    }
}

class NeoForgeGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val mcVersion = data.getUserData(NeoForgeVersionChainStep.MC_VERSION_KEY) ?: return
        val neoforgeVersion = data.getUserData(NeoForgeVersionChainStep.NEOFORGE_VERSION_KEY) ?: return
        val modId = data.getUserData(AbstractModIdStep.KEY) ?: return
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = context.projectJdk?.versionString?.let(JavaVersion::parse)
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val description = data.getUserData(DescriptionStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val parchment = findStep<ParchmentStep>()
        val mcNextVersionPart = mcVersion.parts[1]
        val mcNextVersion = if (mcNextVersionPart is SemanticVersion.Companion.VersionPart.ReleasePart) {
            SemanticVersion.release(1, mcNextVersionPart.version + 1)
        } else {
            mcVersion
        }

        data.putUserData(GRADLE_VERSION_KEY, ngWrapperVersion)

        assets.addTemplateProperties(
            "MOD_ID" to modId,
            "MOD_NAME" to modName,
            "MC_VERSION" to mcVersion,
            "MC_NEXT_VERSION" to mcNextVersion,
            "NEOFORGE_VERSION" to neoforgeVersion,
            "NEOFORGE_SPEC_VERSION" to neoforgeVersion.parts[0].versionString,
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_VERSION" to buildSystemProps.version,
            "DESCRIPTION" to description,
            "AUTHOR_LIST" to authors.joinToString(", "),
            "LICENSE" to license.id,
            "HAS_DATA" to "true",
            "PARCHMENT_VERSION" to parchment.parchmentVersion.takeIf { parchment.useParchment },
        )

        if (javaVersion != null) {
            assets.addTemplateProperties("JAVA_VERSION" to javaVersion.feature)
        }

        if (neoforgeVersion >= SemanticVersion.release(39, 0, 88)) {
            assets.addTemplateProperties("GAME_TEST_FRAMEWORK" to "true")
        }

        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.NEOFORGE_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.NEOFORGE_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.NEOFORGE_SETTINGS_GRADLE_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }

        WriteAction.runAndWait<Throwable> {
            val dir = VfsUtil.createDirectoryIfMissing(
                LocalFileSystem.getInstance(),
                "${assets.outputDirectory}/.gradle",
            )
                ?: throw IllegalStateException("Unable to create .gradle directory")
            val file = dir.findOrCreateChildData(this, MAGIC_RUN_CONFIGS_FILE)
            val fileContents = buildSystemProps.artifactId + "\n" +
                mcVersion + "\n" +
                neoforgeVersion + "\n" +
                "genIntellijRuns"
            VfsUtil.saveText(file, fileContents)
        }
    }
}

class NeoForgeCompileJavaStep(parent: NewProjectWizardStep) : AbstractRunGradleTaskStep(parent) {
    override val task = "compileJava"
}
