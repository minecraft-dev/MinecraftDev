/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.creator

import com.demonwav.mcdev.creator.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.AbstractLatentStep
import com.demonwav.mcdev.creator.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AuthorsStep
import com.demonwav.mcdev.creator.DependStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.MainClassStep
import com.demonwav.mcdev.creator.PlatformVersion
import com.demonwav.mcdev.creator.PluginNameStep
import com.demonwav.mcdev.creator.SimpleMcVersionStep
import com.demonwav.mcdev.creator.SoftDependStep
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addMavenGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.gradle.AbstractPatchGradleFilesStep
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
import com.demonwav.mcdev.creator.getVersionSelector
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStep
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.xml.XmlTag
import kotlinx.coroutines.coroutineScope
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel

class BungeePlatformStep(
    parent: PluginPlatformStep
) : AbstractNewProjectWizardMultiStep<BungeePlatformStep, BungeePlatformStep.Factory>(parent, EP_NAME) {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.bungeePlatformWizard")
    }

    override val self = this
    override val label = "Bungee Platform:"

    class PlatformFactory : PluginPlatformStep.Factory {
        override val name = "BungeeCord"

        override fun createStep(parent: PluginPlatformStep) = BungeePlatformStep(parent)
    }

    interface Factory : NewProjectWizardMultiStepFactory<BungeePlatformStep>
}

abstract class AbstractBungeePlatformStep(
    parent: BungeePlatformStep,
    private val platform: PlatformType
) : AbstractLatentStep<PlatformVersion>(parent) {
    override val description = "download versions"

    override suspend fun computeData() = coroutineScope {
        try {
            asyncIO { getVersionSelector(platform) }.await()
        } catch (e: Throwable) {
            null
        }
    }

    override fun createStep(data: PlatformVersion) =
        SimpleMcVersionStep(this, data.versions.mapNotNull(SemanticVersion::tryParse)).chain(
            ::PluginNameStep,
            ::MainClassStep,
            ::BungeeOptionalSettingsStep,
            ::BungeeBuildSystemStep,
            ::BungeeProjectFilesStep,
            ::BungeePostBuildSystemStep,
        )

    override fun setupProject(project: Project) {
        data.putUserData(KEY, this)
        super.setupProject(project)
    }

    abstract fun getRepositories(mcVersion: SemanticVersion): List<BuildRepository>

    abstract fun getDependencies(mcVersion: SemanticVersion): List<BuildDependency>

    companion object {
        val KEY = Key.create<AbstractBungeePlatformStep>("${AbstractBungeePlatformStep::class.java.name}.platform")
    }
}

class BungeeOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(
        ::AuthorsStep,
        ::DependStep,
        ::SoftDependStep,
    )
}

class BungeeProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating project files"

    override fun setupAssets(project: Project) {
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val (packageName, className) = splitPackage(mainClass)
        val versionRef = data.getUserData(VERSION_REF_KEY) ?: "\${version}"
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val depend = data.getUserData(DependStep.KEY) ?: emptyList()
        val softDepend = data.getUserData(SoftDependStep.KEY) ?: emptyList()

        assets.addTemplateProperties(
            "PACKAGE" to packageName,
            "CLASS_NAME" to className,
            "MAIN" to mainClass,
            "VERSION" to versionRef,
            "NAME" to pluginName,
        )

        if (authors.isNotEmpty()) {
            assets.addTemplateProperties(
                "AUTHOR" to authors.joinToString(", ")
            )
        }
        if (description.isNotBlank()) {
            assets.addTemplateProperties(
                "DESCRIPTION" to description
            )
        }
        if (depend.isNotEmpty()) {
            assets.addTemplateProperties(
                "DEPEND" to depend
            )
        }
        if (softDepend.isNotEmpty()) {
            assets.addTemplateProperties(
                "SOFT_DEPEND" to softDepend
            )
        }

        assets.addTemplates(
            project,
            "src/main/resources/bungee.yml" to MinecraftTemplates.BUNGEECORD_PLUGIN_YML_TEMPLATE,
            "src/main/java/${mainClass.replace('.', '/')}.java" to MinecraftTemplates.BUNGEECORD_MAIN_CLASS_TEMPLATE,
        )
    }

    companion object {
        val VERSION_REF_KEY = Key.create<String>("${BungeeProjectFilesStep::class.java.name}.versionRef")
    }
}

class BungeeBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "BungeeCord"
}

class BungeePostBuildSystemStep(
    parent: NewProjectWizardStep
) : AbstractRunBuildSystemStep(parent, BungeeBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}

class BungeeGradleSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> BungeeGradleFilesStep(parent).chain(
                ::BungeePatchBuildGradleStep,
                ::GradleWrapperStep
            )
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent).chain(::ReformatBuildGradleStep)
            else -> EmptyStep(parent)
        }
    }
}

class BungeeGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val projectName = baseData.name
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        assets.addTemplateProperties(
            "PROJECT_NAME" to projectName,
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "PLUGIN_VERSION" to buildSystemProps.version,
        )
        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.BUNGEECORD_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.BUNGEECORD_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.BUNGEECORD_SETTINGS_GRADLE_TEMPLATE,
        )
        assets.addGradleWrapperProperties(project)

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }
    }
}

class BungeePatchBuildGradleStep(parent: NewProjectWizardStep) : AbstractPatchGradleFilesStep(parent) {
    override fun patch(project: Project, gradleFiles: GradleFiles) {
        val platform = data.getUserData(AbstractBungeePlatformStep.KEY) ?: return
        val mcVersion = data.getUserData(SimpleMcVersionStep.KEY) ?: return
        val repositories = platform.getRepositories(mcVersion)
        val dependencies = platform.getDependencies(mcVersion)
        addRepositories(project, gradleFiles.buildGradle, repositories)
        addDependencies(project, gradleFiles.buildGradle, dependencies)
    }
}

class BungeeMavenSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> BungeeMavenFilesStep(parent).chain(::BungeePatchPomStep)
            BuildSystemSupport.POST_STEP -> MavenImportStep(parent).chain(::ReformatPomStep)
            else -> EmptyStep(parent)
        }
    }
}

class BungeeMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        data.putUserData(BungeeProjectFilesStep.VERSION_REF_KEY, "\${project.version}")
        assets.addDefaultMavenProperties()
        assets.addTemplates(project, "pom.xml" to MinecraftTemplates.BUNGEECORD_POM_TEMPLATE)
        if (gitEnabled) {
            assets.addMavenGitignore(project)
        }
    }
}

class BungeePatchPomStep(parent: NewProjectWizardStep) : AbstractPatchPomStep(parent) {
    override fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        super.patchPom(model, root)
        val platform = data.getUserData(AbstractBungeePlatformStep.KEY) ?: return
        val mcVersion = data.getUserData(SimpleMcVersionStep.KEY) ?: return
        val repositories = platform.getRepositories(mcVersion)
        val dependencies = platform.getDependencies(mcVersion)
        setupDependencies(model, repositories, dependencies)
    }
}

class BungeeMainPlatformStep(parent: BungeePlatformStep) : AbstractBungeePlatformStep(parent, PlatformType.BUNGEECORD) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/")
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "net.md-5",
            "bungeecord-api",
            mcVersion.toString(),
            mavenScope = "provided",
            gradleConfiguration = "compileOnly"
        )
    )

    class Factory : BungeePlatformStep.Factory {
        override val name = "BungeeCord"

        override fun createStep(parent: BungeePlatformStep) = BungeeMainPlatformStep(parent)
    }
}

class WaterfallPlatformStep(parent: BungeePlatformStep) : AbstractBungeePlatformStep(parent, PlatformType.WATERFALL) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/"),
        BuildRepository(
            "papermc-repo",
            "https://repo.papermc.io/repository/maven-public/"
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "io.github.waterfallmc",
            "waterfall-api",
            "$mcVersion-SNAPSHOT",
            mavenScope = "provided",
            gradleConfiguration = "compileOnly"
        ),
    )

    class Factory : BungeePlatformStep.Factory {
        override val name = "Waterfall"

        override fun createStep(parent: BungeePlatformStep) = WaterfallPlatformStep(parent)
    }
}
