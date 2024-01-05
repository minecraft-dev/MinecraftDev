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

import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModIdStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AbstractReformatFilesStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.UpdateUrlStep
import com.demonwav.mcdev.creator.step.UseMixinsStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class NeoForgeProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating NeoForge project files"

    override fun setupAssets(project: Project) {
        val mcVersion = data.getUserData(NeoForgeVersionChainStep.MC_VERSION_KEY) ?: return
        val forgeVersion = data.getUserData(NeoForgeVersionChainStep.NEOFORGE_VERSION_KEY) ?: return
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val (mainPackageName, mainClassName) = splitPackage(mainClass)
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modId = data.getUserData(AbstractModIdStep.KEY) ?: return
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val updateUrl = data.getUserData(UpdateUrlStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false

        val nextMcVersion = when (val part = mcVersion.parts.getOrNull(1)) {
            // Mimics the code used to get the next Minecraft version in NeoForge's MDK
            // https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/build.gradle#L884
            is SemanticVersion.Companion.VersionPart.ReleasePart -> (part.version + 1).toString()
            null -> "?"
            else -> part.versionString
        }

        val packDescriptor = ForgePackDescriptor.forMcVersion(mcVersion) ?: ForgePackDescriptor.FORMAT_3

        assets.addTemplateProperties(
            "PACKAGE_NAME" to mainPackageName,
            "CLASS_NAME" to mainClassName,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_ID" to modId,
            "MOD_NAME" to modName,
            "MOD_VERSION" to buildSystemProps.version,
            "NEOFORGE_SPEC_VERSION" to forgeVersion.parts[0].versionString,
            "MC_VERSION" to mcVersion,
            "MC_NEXT_VERSION" to "1.$nextMcVersion",
            "LICENSE" to license,
            "DESCRIPTION" to description,
            "MIXIN_CONFIG" to if (useMixins) "$modId.mixins.json" else null,
            "PACK_FORMAT" to packDescriptor.format,
            "PACK_COMMENT" to packDescriptor.comment,
        )

        if (updateUrl.isNotBlank()) {
            assets.addTemplateProperties("UPDATE_URL" to updateUrl)
        }

        if (authors.isNotEmpty()) {
            assets.addTemplateProperties("AUTHOR_LIST" to authors.joinToString(", "))
        }

        if (website.isNotBlank()) {
            assets.addTemplateProperties("WEBSITE" to website)
        }

        val mainClassTemplate = MinecraftTemplates.NEOFORGE_MAIN_CLASS_TEMPLATE

        assets.addTemplates(
            project,
            "src/main/java/${mainClass.replace('.', '/')}.java" to mainClassTemplate,
            "src/main/resources/pack.mcmeta" to MinecraftTemplates.NEOFORGE_PACK_MCMETA_TEMPLATE,
            "src/main/resources/META-INF/mods.toml" to MinecraftTemplates.NEOFORGE_MODS_TOML_TEMPLATE,
        )

        val configTemplate = MinecraftTemplates.NEOFORGE_CONFIG_TEMPLATE

        if (configTemplate != null) {
            val configPath = if (mainPackageName != null) {
                "src/main/java/${mainPackageName.replace('.', '/')}/Config.java"
            } else {
                "src/main/java/Config.java"
            }
            assets.addTemplates(project, configPath to configTemplate)
        }

        assets.addLicense(project)
    }
}

// Needs to be a separate step from above because of PACKAGE_NAME being different
class NeoForgeMixinsJsonStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating mixins json"

    override fun setupAssets(project: Project) {
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false
        if (useMixins) {
            val modId = data.getUserData(AbstractModIdStep.KEY) ?: return
            val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
            assets.addTemplateProperties(
                "PACKAGE_NAME" to "${buildSystemProps.groupId}.$modId.mixin",
                "MOD_ID" to buildSystemProps.artifactId,
            )
            val mixinsJsonFile = "src/main/resources/$modId.mixins.json"
            assets.addTemplates(project, mixinsJsonFile to MinecraftTemplates.NEOFORGE_MIXINS_JSON_TEMPLATE)
        }
    }
}

class NeoForgeReformatPackDescriptorStep(parent: NewProjectWizardStep) : AbstractReformatFilesStep(parent) {

    override fun addFilesToReformat() {
        addFileToReformat("src/main/resources/pack.mcmeta")
    }
}

class NeoForgeBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "NeoForge"
}

class NeoForgePostBuildSystemStep(
    parent: NewProjectWizardStep,
) : AbstractRunBuildSystemStep(parent, NeoForgeBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}
