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

package com.demonwav.mcdev.platform.forge.creator

import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AbstractReformatFilesStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.UpdateUrlStep
import com.demonwav.mcdev.creator.step.UseMixinsStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.forge.util.ForgePackAdditionalData
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class ForgeProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Forge project files"

    override fun setupAssets(project: Project) {
        val mcVersion = data.getUserData(ForgeVersionChainStep.MC_VERSION_KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionChainStep.FORGE_VERSION_KEY) ?: return
        val (mainPackageName, mainClassName) = splitPackage(data.getUserData(MainClassStep.KEY) ?: return)
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val updateUrl = data.getUserData(UpdateUrlStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val website = data.getUserData(WebsiteStep.KEY) ?: ""

        val nextMcVersion = when (val part = mcVersion.parts.getOrNull(1)) {
            // Mimics the code used to get the next Minecraft version in Forge's MDK
            // https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/build.gradle#L884
            is SemanticVersion.Companion.VersionPart.ReleasePart -> (part.version + 1).toString()
            null -> "?"
            else -> part.versionString
        }

        val packDescriptor = ForgePackDescriptor.forMcVersion(mcVersion) ?: ForgePackDescriptor.FORMAT_3
        val additionalPackData = ForgePackAdditionalData.forMcVersion(mcVersion)

        assets.addTemplateProperties(
            "PACKAGE_NAME" to mainPackageName,
            "CLASS_NAME" to mainClassName,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_NAME" to modName,
            "MOD_VERSION" to buildSystemProps.version,
            "DISPLAY_TEST" to (forgeVersion >= ForgeConstants.DISPLAY_TEST_MANIFEST_VERSION),
            "FORGE_SPEC_VERSION" to forgeVersion.parts[0].versionString,
            "MC_VERSION" to mcVersion,
            "MC_NEXT_VERSION" to "1.$nextMcVersion",
            "LICENSE" to license,
            "DESCRIPTION" to description,
            "PACK_FORMAT" to packDescriptor.format,
            "PACK_COMMENT" to packDescriptor.comment,
            "FORGE_DATA" to (additionalPackData ?: ""),
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

        val mainClassTemplate = when {
            mcVersion >= MinecraftVersions.MC1_20 -> MinecraftTemplates.FG3_1_20_MAIN_CLASS_TEMPLATE
            mcVersion >= MinecraftVersions.MC1_19_3 -> MinecraftTemplates.FG3_1_19_3_MAIN_CLASS_TEMPLATE
            mcVersion >= MinecraftVersions.MC1_19 -> MinecraftTemplates.FG3_1_19_MAIN_CLASS_TEMPLATE
            mcVersion >= MinecraftVersions.MC1_18 -> MinecraftTemplates.FG3_1_18_MAIN_CLASS_TEMPLATE
            mcVersion >= MinecraftVersions.MC1_17 -> MinecraftTemplates.FG3_1_17_MAIN_CLASS_TEMPLATE
            else -> MinecraftTemplates.FG3_MAIN_CLASS_TEMPLATE
        }

        assets.addTemplates(
            project,
            "src/main/java/${mainPackageName.replace('.', '/')}/$mainClassName.java" to mainClassTemplate,
            "src/main/resources/pack.mcmeta" to MinecraftTemplates.PACK_MCMETA_TEMPLATE,
            "src/main/resources/META-INF/mods.toml" to MinecraftTemplates.MODS_TOML_TEMPLATE,
        )

        val configTemplate = when {
            mcVersion >= MinecraftVersions.MC1_20 -> MinecraftTemplates.FG3_1_20_CONFIG_TEMPLATE
            else -> null
        }

        if (configTemplate != null) {
            assets.addTemplates(
                project,
                "src/main/java/${mainPackageName.replace('.', '/')}/Config.java" to configTemplate,
            )
        }

        assets.addLicense(project)
    }
}

// Needs to be a separate step from above because of PACKAGE_NAME being different
class ForgeMixinsJsonStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating mixins json"

    override fun setupAssets(project: Project) {
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false
        if (useMixins) {
            val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
            assets.addTemplateProperties(
                "PACKAGE_NAME" to "${buildSystemProps.groupId}.${buildSystemProps.artifactId}.mixin",
                "ARTIFACT_ID" to buildSystemProps.artifactId,
            )
            val mixinsJsonFile = "src/main/resources/${buildSystemProps.artifactId}.mixins.json"
            assets.addTemplates(project, mixinsJsonFile to MinecraftTemplates.FORGE_MIXINS_JSON_TEMPLATE)
        }
    }
}

class ForgeReformatPackDescriptorStep(parent: NewProjectWizardStep) : AbstractReformatFilesStep(parent) {

    override fun addFilesToReformat() {
        addFileToReformat("src/main/resources/pack.mcmeta")
    }
}

class ForgeBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Forge"
}

class ForgePostBuildSystemStep(
    parent: NewProjectWizardStep,
) : AbstractRunBuildSystemStep(parent, ForgeBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}
