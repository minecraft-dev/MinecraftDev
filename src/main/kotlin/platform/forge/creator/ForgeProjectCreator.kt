/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.creator

import com.demonwav.mcdev.creator.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.AbstractLatentStep
import com.demonwav.mcdev.creator.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AbstractSelectMcVersionThenForkStep
import com.demonwav.mcdev.creator.AbstractSelectVersionStep
import com.demonwav.mcdev.creator.AuthorsStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.LicenseStep
import com.demonwav.mcdev.creator.MainClassStep
import com.demonwav.mcdev.creator.ModNameStep
import com.demonwav.mcdev.creator.UpdateUrlStep
import com.demonwav.mcdev.creator.UseMixinsStep
import com.demonwav.mcdev.creator.WebsiteStep
import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.gradle.AbstractRunGradleTaskStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GRADLE_VERSION_KEY
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.gradle.addGradleWrapperProperties
import com.demonwav.mcdev.creator.chain
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.platformtype.ModPlatformStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.forge.util.ForgePackAdditionalData
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.lang.JavaVersion
import java.util.Locale
import kotlinx.coroutines.coroutineScope

private val minSupportedMcVersion = MinecraftVersions.MC1_16_5
private val fg5WrapperVersion = SemanticVersion.release(7, 5, 1)

class ForgePlatformStep(parent: ModPlatformStep) : AbstractLatentStep<ForgeVersion>(parent) {
    override val description = "fetch Forge versions"

    override suspend fun computeData() = coroutineScope {
        asyncIO { ForgeVersion.downloadData() }.await()
    }

    override fun createStep(data: ForgeVersion) = ForgeMcVersionStep(this, data)
        .chain(
            ::ModNameStep,
            ::MainClassStep,
            ::UseMixinsStep,
            ::LicenseStep,
            ::ForgeOptionalSettingsStep,
            ::ForgeBuildSystemStep,
            ::ForgeProjectFilesStep,
            ::ForgeMixinsJsonStep,
            ::ForgeCompileJavaStep,
            ::ForgePostBuildSystemStep,
        )

    class Factory : ModPlatformStep.Factory {
        override val name = "Forge"
        override fun createStep(parent: ModPlatformStep) = ForgePlatformStep(parent)
    }
}

class ForgeMcVersionStep(
    parent: NewProjectWizardStep,
    private val forgeVersionData: ForgeVersion
) : AbstractSelectMcVersionThenForkStep<SemanticVersion>(
    parent,
    forgeVersionData.sortedMcVersions.filter { it >= minSupportedMcVersion }
) {
    override val label = "Minecraft Version:"

    override fun initStep(version: SemanticVersion) = ForgeVersionStep(this, forgeVersionData.getForgeVersions(version))

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(step))
        super.setupProject(project)
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${ForgeMcVersionStep::class.java.name}.version")
    }
}

class ForgeVersionStep(
    parent: NewProjectWizardStep,
    versions: List<SemanticVersion>
) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Forge Version:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${ForgeVersionStep::class.java.name}.version")
    }
}

class ForgeOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(::AuthorsStep, ::WebsiteStep, ::UpdateUrlStep)
}

class ForgeGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    private fun transformModName(modName: String): String {
        return modName.lowercase(Locale.ENGLISH).replace(" ", "")
    }

    override fun setupAssets(project: Project) {
        val mcVersion = data.getUserData(ForgeMcVersionStep.KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionStep.KEY) ?: return
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
    }
}

class ForgeProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Forge project files"

    override fun setupAssets(project: Project) {
        val mcVersion = data.getUserData(ForgeMcVersionStep.KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionStep.KEY) ?: return
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
            ".gitignore" to MinecraftTemplates.GRADLE_GITIGNORE_TEMPLATE,
        )

        assets.addLicense(project)

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

class ForgeCompileJavaStep(parent: NewProjectWizardStep) : AbstractRunGradleTaskStep(parent) {
    override val task = "compileJava"
}

class ForgeBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Forge"
}

class ForgePostBuildSystemStep(
    parent: NewProjectWizardStep
) : AbstractRunBuildSystemStep(parent, ForgeBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}

class ForgeGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> ForgeGradleFilesStep(parent).chain(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent)
            else -> EmptyStep(parent)
        }
    }
}

const val MAGIC_RUN_CONFIGS_FILE = ".hello_from_mcdev"
