/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.AbstractLatentStep
import com.demonwav.mcdev.creator.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AbstractSelectVersionStep
import com.demonwav.mcdev.creator.AuthorsStep
import com.demonwav.mcdev.creator.DependStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.LicenseStep
import com.demonwav.mcdev.creator.MainClassStep
import com.demonwav.mcdev.creator.PluginNameStep
import com.demonwav.mcdev.creator.WebsiteStep
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addMavenGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.gradle.GRADLE_VERSION_KEY
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.gradle.ReformatBuildGradleStep
import com.demonwav.mcdev.creator.buildsystem.gradle.addGradleWrapperProperties
import com.demonwav.mcdev.creator.buildsystem.maven.AbstractPatchPomStep
import com.demonwav.mcdev.creator.buildsystem.maven.MavenImportStep
import com.demonwav.mcdev.creator.buildsystem.maven.ReformatPomStep
import com.demonwav.mcdev.creator.buildsystem.maven.addDefaultMavenProperties
import com.demonwav.mcdev.creator.chain
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.platform.sponge.SpongeVersion
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.onShown
import com.intellij.ide.starters.local.GeneratorEmptyDirectory
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.util.Key
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.dsl.builder.Panel
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
        ::SpongeProjectFilesStep,
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

    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        versionProperty.afterChange {
            applyJdkVersion()
        }
        versionBox.onShown {
            applyJdkVersion()
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
        applyJdkVersion()
    }

    private fun applyJdkVersion() {
        SemanticVersion.tryParse(version)?.let { version ->
            val preferredJdk = when {
                version >= SpongeConstants.API9 -> JavaSdkVersion.JDK_17
                else -> JavaSdkVersion.JDK_1_8
            }
            findStep<JdkProjectSetupFinalizer>().setPreferredJdk(preferredJdk, "Sponge $version")
        }
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
        ::DependStep,
    )
}

class SpongeProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
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
        assets.addLicense(project)
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
            BuildSystemSupport.POST_STEP -> SpongeGradleImportStep(parent).chain(::ReformatBuildGradleStep)
            else -> EmptyStep(parent)
        }
    }
}

class SpongeGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        data.putUserData(GRADLE_VERSION_KEY, SemanticVersion.release(7, 4, 2))

        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val spongeVersion = data.getUserData(SpongeApiVersionStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val dependencies = data.getUserData(DependStep.KEY) ?: emptyList()
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
            "AUTHORS" to authors,
            "DEPENDENCIES" to dependencies,
            "PROJECT_NAME" to baseData.name,
        )

        if (description.isNotBlank()) {
            assets.addTemplateProperties("DESCRIPTION" to description)
        }

        if (website.isNotBlank()) {
            assets.addTemplateProperties("WEBSITE" to website)
        }

        assets.addTemplates(
            project,
            "build.gradle.kts" to MinecraftTemplates.SPONGE8_BUILD_GRADLE_TEMPLATE,
            "settings.gradle.kts" to MinecraftTemplates.SPONGE8_SETTINGS_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.SPONGE8_GRADLE_PROPERTIES_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)

        assets.addAssets(
            GeneratorEmptyDirectory("src/main/java"),
            GeneratorEmptyDirectory("src/main/resources"),
        )

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }
    }
}

class SpongeGradleImportStep(parent: NewProjectWizardStep) : GradleImportStep(parent) {
    override val additionalRunTasks = listOf("runServer")
}

class SpongeMavenSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> SpongeMavenFilesStep(parent).chain(::SpongePatchPomStep)
            BuildSystemSupport.POST_STEP -> SpongeMavenProjectFilesStep(parent).chain(
                ::MavenImportStep,
                ::ReformatPomStep
            )
            else -> EmptyStep(parent)
        }
    }
}

class SpongeMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        assets.addDefaultMavenProperties()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        assets.addTemplateProperties("JAVA_VERSION" to javaVersion)
        assets.addTemplates(project, "pom.xml" to MinecraftTemplates.SPONGE_POM_TEMPLATE)
        if (gitEnabled) {
            assets.addMavenGitignore(project)
        }
    }
}

class SpongeMavenProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven project files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val spongeApiVersion = data.getUserData(SpongeApiVersionStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val dependencies = data.getUserData(DependStep.KEY) ?: emptyList()

        assets.addTemplateProperties(
            "PLUGIN_ID" to buildSystemProps.artifactId,
            "VERSION_PLACEHOLDER" to "\${version}",
            "SPONGEAPI_VERSION" to spongeApiVersion,
            "LICENSE" to license.id,
            "PLUGIN_NAME" to pluginName,
            "MAIN_CLASS" to mainClass,
            "DESCRIPTION" to description,
            "WEBSITE" to website,
            "AUTHORS" to authors,
            "DEPENDENCIES" to dependencies,
        )
        assets.addTemplates(
            project,
            "src/main/resources/META-INF/sponge_plugins.json" to MinecraftTemplates.SPONGE8_PLUGINS_JSON_TEMPLATE,
        )
        assets.addLicense(project)
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
