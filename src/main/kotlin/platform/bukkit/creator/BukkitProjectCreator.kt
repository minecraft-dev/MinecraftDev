/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.creator

import com.demonwav.mcdev.creator.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.AbstractLatentStep
import com.demonwav.mcdev.creator.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AbstractOptionalStringStep
import com.demonwav.mcdev.creator.AuthorsStep
import com.demonwav.mcdev.creator.DependStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.MainClassStep
import com.demonwav.mcdev.creator.PlatformVersion
import com.demonwav.mcdev.creator.PluginNameStep
import com.demonwav.mcdev.creator.SimpleMcVersionStep
import com.demonwav.mcdev.creator.SoftDependStep
import com.demonwav.mcdev.creator.WebsiteStep
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
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.BukkitModuleType
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStep
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.coroutineScope
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel

class BukkitPlatformStep(
    parent: PluginPlatformStep
) : AbstractNewProjectWizardMultiStep<BukkitPlatformStep, BukkitPlatformStep.Factory>(parent, EP_NAME) {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.bukkitPlatformWizard")
    }

    override val self = this
    override val label = "Bukkit Platform:"

    class PlatformFactory : PluginPlatformStep.Factory {
        override val name = "Bukkit"

        override fun createStep(parent: PluginPlatformStep) = BukkitPlatformStep(parent)
    }

    interface Factory : NewProjectWizardMultiStepFactory<BukkitPlatformStep>
}

abstract class AbstractBukkitPlatformStep(
    parent: BukkitPlatformStep,
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
            ::BukkitOptionalSettingsStep,
            ::BukkitBuildSystemStep,
            ::BukkitProjectFilesStep,
            ::BukkitPostBuildSystemStep,
        )

    override fun setupProject(project: Project) {
        data.putUserData(KEY, this)
        super.setupProject(project)
    }

    abstract fun getRepositories(mcVersion: SemanticVersion): List<BuildRepository>

    abstract fun getDependencies(mcVersion: SemanticVersion): List<BuildDependency>

    companion object {
        val KEY = Key.create<AbstractBukkitPlatformStep>("${AbstractBukkitPlatformStep::class.java.name}.platform")
    }
}

class BukkitOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(
        ::AuthorsStep,
        ::WebsiteStep,
        ::BukkitLogPrefixStep,
        ::BukkitLoadOrderStep,
        ::BukkitLoadBeforeStep,
        ::DependStep,
        ::SoftDependStep,
    )
}

class BukkitLogPrefixStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Log Prefix:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${BukkitLogPrefixStep::class.java.name}.logPrefix")
    }
}

class BukkitLoadOrderStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val loadOrderProperty = propertyGraph.property(LoadOrder.POSTWORLD)
    private var loadOrder by loadOrderProperty

    init {
        loadOrderProperty.transform(LoadOrder::name, LoadOrder::valueOf)
            .bindStorage("${javaClass.name}.loadOrder")
    }

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("Load at:") {
                segmentedButton(LoadOrder.values().toList(), LoadOrder::toString)
                    .bind(loadOrderProperty)
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, loadOrder)
    }

    companion object {
        val KEY = Key.create<LoadOrder>("${BukkitLoadOrderStep::class.java.name}.loadOrder")
    }
}

class BukkitLoadBeforeStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Load Before:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, AuthorsStep.parseAuthors(value))
    }

    companion object {
        val KEY = Key.create<List<String>>("${BukkitLoadBeforeStep::class.java.name}.loadBefore")
    }
}

class BukkitProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating project files"

    override fun setupAssets(project: Project) {
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val versionRef = data.getUserData(VERSION_REF_KEY) ?: "\${version}"
        val prefix = data.getUserData(BukkitLogPrefixStep.KEY) ?: ""
        val loadOrder = data.getUserData(BukkitLoadOrderStep.KEY) ?: return
        val loadBefore = data.getUserData(BukkitLoadBeforeStep.KEY) ?: emptyList()
        val deps = data.getUserData(DependStep.KEY) ?: emptyList()
        val softDeps = data.getUserData(SoftDependStep.KEY) ?: emptyList()
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val mcVersion = data.getUserData(SimpleMcVersionStep.KEY) ?: return

        val (packageName, className) = splitPackage(mainClass)

        assets.addTemplateProperties(
            "MAIN" to mainClass,
            "VERSION" to versionRef,
            "NAME" to pluginName,
            "PACKAGE" to packageName,
            "CLASS_NAME" to className,
        )

        if (prefix.isNotBlank()) {
            assets.addTemplateProperties("PREFIX" to prefix)
        }

        if (loadOrder != LoadOrder.POSTWORLD) {
            assets.addTemplateProperties("LOAD" to loadOrder.name)
        }

        if (loadBefore.isNotEmpty()) {
            assets.addTemplateProperties("LOAD_BEFORE" to loadBefore)
        }

        if (deps.isNotEmpty()) {
            assets.addTemplateProperties("DEPEND" to deps)
        }

        if (softDeps.isNotEmpty()) {
            assets.addTemplateProperties("SOFT_DEPEND" to softDeps)
        }

        if (authors.isNotEmpty()) {
            assets.addTemplateProperties("AUTHOR_LIST" to authors)
        }

        if (description.isNotBlank()) {
            assets.addTemplateProperties("DESCRIPTION" to description)
        }

        if (website.isNotEmpty()) {
            assets.addTemplateProperties("WEBSITE" to website)
        }

        if (mcVersion >= BukkitModuleType.API_TAG_VERSION) {
            assets.addTemplateProperties("API_VERSION" to mcVersion.take(2))
        }

        assets.addTemplates(
            project,
            "src/main/resources/plugin.yml" to MinecraftTemplates.BUKKIT_PLUGIN_YML_TEMPLATE,
            "src/main/java/${mainClass.replace('.', '/')}.java" to MinecraftTemplates.BUKKIT_MAIN_CLASS_TEMPLATE,
        )
    }

    companion object {
        val VERSION_REF_KEY = Key.create<String>("${BukkitProjectFilesStep::class.java.name}.versionRef")
    }
}

class BukkitBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Bukkit"
}

class BukkitPostBuildSystemStep(
    parent: NewProjectWizardStep
) : AbstractRunBuildSystemStep(parent, BukkitBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}

class BukkitGradleSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> BukkitGradleFilesStep(parent).chain(
                ::BukkitPatchBuildGradleStep,
                ::GradleWrapperStep
            )
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent).chain(::ReformatBuildGradleStep)
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

class BukkitMavenSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> BukkitMavenFilesStep(parent).chain(::BukkitPatchPomStep)
            BuildSystemSupport.POST_STEP -> MavenImportStep(parent).chain(::ReformatPomStep)
            else -> EmptyStep(parent)
        }
    }
}

class BukkitMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        data.putUserData(BukkitProjectFilesStep.VERSION_REF_KEY, "\${project.version}")
        assets.addDefaultMavenProperties()
        assets.addTemplates(project, "pom.xml" to MinecraftTemplates.BUKKIT_POM_TEMPLATE)
    }
}

class BukkitPatchPomStep(parent: NewProjectWizardStep) : AbstractPatchPomStep(parent) {
    override fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        super.patchPom(model, root)
        val platform = data.getUserData(AbstractBukkitPlatformStep.KEY) ?: return
        val mcVersion = data.getUserData(SimpleMcVersionStep.KEY) ?: return
        val repositories = platform.getRepositories(mcVersion)
        val dependencies = platform.getDependencies(mcVersion)
        setupDependencies(model, repositories, dependencies)
    }
}

class SpigotPlatformStep(parent: BukkitPlatformStep) : AbstractBukkitPlatformStep(parent, PlatformType.SPIGOT) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository(
            "spigotmc-repo",
            "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
        ),
        BuildRepository(
            "sonatype",
            "https://oss.sonatype.org/content/groups/public/"
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "org.spigotmc",
            "spigot-api",
            "$mcVersion-R0.1-SNAPSHOT",
            mavenScope = "provided",
            gradleConfiguration = "compileOnly",
        )
    )

    class Factory : BukkitPlatformStep.Factory {
        override val name = "Spigot"

        override fun createStep(parent: BukkitPlatformStep) = SpigotPlatformStep(parent)
    }
}

class PaperPlatformStep(parent: BukkitPlatformStep) : AbstractBukkitPlatformStep(parent, PlatformType.PAPER) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository(
            "papermc-repo",
            "https://repo.papermc.io/repository/maven-public/"
        ),
        BuildRepository(
            "sonatype",
            "https://oss.sonatype.org/content/groups/public/"
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion): List<BuildDependency> {
        val paperGroupId = when {
            mcVersion >= MinecraftVersions.MC1_17 -> "io.papermc.paper"
            else -> "com.destroystokyo.paper"
        }
        return listOf(
            BuildDependency(
                paperGroupId,
                "paper-api",
                "$mcVersion-R0.1-SNAPSHOT",
                mavenScope = "provided",
                gradleConfiguration = "compileOnly"
            )
        )
    }

    class Factory : BukkitPlatformStep.Factory {
        override val name = "Paper"

        override fun createStep(parent: BukkitPlatformStep) = PaperPlatformStep(parent)
    }
}
