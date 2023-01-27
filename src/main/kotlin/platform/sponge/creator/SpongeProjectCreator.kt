/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.AbstractLatentStep
import com.demonwav.mcdev.creator.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AbstractOptionalStringStep
import com.demonwav.mcdev.creator.AbstractSelectVersionStep
import com.demonwav.mcdev.creator.AuthorsStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.LicenseStep
import com.demonwav.mcdev.creator.MainClassStep
import com.demonwav.mcdev.creator.PluginNameStep
import com.demonwav.mcdev.creator.WebsiteStep
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.gradle.addGradleWrapperProperties
import com.demonwav.mcdev.creator.buildsystem.maven.AbstractPatchPomStep
import com.demonwav.mcdev.creator.buildsystem.maven.MavenImportStep
import com.demonwav.mcdev.creator.buildsystem.maven.ReformatPomStep
import com.demonwav.mcdev.creator.buildsystem.maven.addDefaultMavenProperties
import com.demonwav.mcdev.creator.chain
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.platform.sponge.SpongeVersion
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.xml.XmlTag
import java.util.EnumSet
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel

class SpongePlatformStep(parent: PluginPlatformStep) : AbstractLatentStep<SpongeVersion>(parent) {
    override val description = "download Sponge versions"

    override suspend fun computeData() = SpongeVersion.downloadData()

    override fun createStep(data: SpongeVersion) = SpongeApiVersionStep(this, data).chain(
        ::PluginNameStep,
        ::MainClassStep,
        ::LicenseStep,
        ::SpongeOptionalSettingsStep,
        ::SpongeBuildSystemStep,
        ::SpongeMainClassStep,
        ::SpongePostBuildSystemStep,
    )

    class Factory : PluginPlatformStep.Factory {
        override val name = "Sponge"

        override fun createStep(parent: PluginPlatformStep) = SpongePlatformStep(parent)
    }
}

class SpongeApiVersionStep(
    parent: NewProjectWizardStep,
    data: SpongeVersion
) : AbstractSelectVersionStep<SemanticVersion>(parent, data.versions.keys.mapNotNull(SemanticVersion::tryParse)) {
    override val label = "Sponge API Version:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${SpongeApiVersionStep::class.java.name}.version")
    }
}

class SpongeOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(
        ::AuthorsStep,
        ::WebsiteStep,
        ::SpongeDependStep,
    )
}

class SpongeDependStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Depend:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${SpongeDependStep::class.java.name}.depend")
    }
}

class SpongeMainClassStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating project files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val (packageName, className) = splitPackage(data.getUserData(MainClassStep.KEY) ?: return)

        assets.addTemplateProperties(
            "PLUGIN_ID" to buildSystemProps.artifactId,
            "PACKAGE" to packageName,
            "CLASS_NAME" to className,
        )
        val mainClassFile = "src/main/java/${packageName.replace('.', '/')}/$className.java"
        assets.addTemplates(
            project,
            mainClassFile to MinecraftTemplates.SPONGE8_MAIN_CLASS_TEMPLATE,
        )
    }
}

class SpongeBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Sponge"
}

class SpongePostBuildSystemStep(
    parent: NewProjectWizardStep
) : AbstractRunBuildSystemStep(parent, SpongeBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}

class SpongeGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> SpongeGradleFilesStep(parent).chain(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent)
            else -> EmptyStep(parent)
        }
    }
}

class SpongeGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().minVersion.ordinal
        val spongeVersion = data.getUserData(SpongeApiVersionStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val dependencies = data.getUserData(SpongeDependStep.KEY)?.let(AuthorsStep::parseAuthors) ?: emptyList()
        val baseData = data.getUserData(NewProjectWizardBaseData.KEY) ?: return

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "PLUGIN_ID" to buildSystemProps.artifactId,
            "PLUGIN_VERSION" to buildSystemProps.version,
            "JAVA_VERSION" to javaVersion,
            "SPONGEAPI_VERSION" to spongeVersion,
            "LICENSE" to license.id,
            "PLUGIN_NAME" to pluginName,
            "MAIN_CLASS" to mainClass,
            "DESCRIPTION" to description,
            "WEBSITE" to website,
            "AUTHORS" to authors,
            "DEPENDENCIES" to dependencies,
            "PROJECT_NAME" to baseData.name,
        )

        assets.addTemplates(
            project,
            "build.gradle.kts" to MinecraftTemplates.SPONGE8_BUILD_GRADLE_TEMPLATE,
            "settings.gradle.kts" to MinecraftTemplates.SPONGE8_SETTINGS_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.SPONGE8_GRADLE_PROPERTIES_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)
    }
}

class SpongeMavenSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> SpongeMavenFilesStep(parent).chain(::SpongePatchPomStep)
            BuildSystemSupport.POST_STEP -> MavenImportStep(parent).chain(::ReformatPomStep)
            else -> EmptyStep(parent)
        }
    }
}

class SpongeMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        assets.addDefaultMavenProperties()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().minVersion.ordinal
        assets.addTemplateProperties("JAVA_VERSION" to javaVersion)
        assets.addTemplates(project, "pom.xml" to MinecraftTemplates.SPONGE_POM_TEMPLATE)
    }
}

class SpongePatchPomStep(parent: NewProjectWizardStep) : AbstractPatchPomStep(parent) {
    override fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        super.patchPom(model, root)
        val spongeApiVersion = data.getUserData(SpongeApiVersionStep.KEY) ?: return
        setupDependencies(
            model,
            listOf(
                BuildRepository(
                    "spongepowered-repo",
                    "https://repo.spongepowered.org/maven/",
                    buildSystems = EnumSet.of(BuildSystemType.MAVEN)
                )
            ),
            listOf(
                BuildDependency(
                    "org.spongepowered",
                    "spongeapi",
                    spongeApiVersion.toString(),
                    mavenScope = "provided"
                )
            )
        )
    }
}
