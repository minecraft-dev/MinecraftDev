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

import com.demonwav.mcdev.creator.*
import com.demonwav.mcdev.creator.buildsystem.*
import com.demonwav.mcdev.creator.buildsystem.gradle.*
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.*
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.platform.sponge.SpongeVersion
import com.demonwav.mcdev.util.*
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlTag
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.EnumSet
import java.util.Locale
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

class SpongeApiVersionStep(parent: NewProjectWizardStep, data: SpongeVersion) : AbstractSelectVersionStep<SemanticVersion>(parent, data.versions.keys.mapNotNull(SemanticVersion::tryParse)) {
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
        assets.addTemplates(
            project,
            "src/main/java/${packageName.replace('.', '/')}/$className.java" to MinecraftTemplates.SPONGE8_MAIN_CLASS_TEMPLATE,
        )
    }
}

class SpongeBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Sponge"
}

class SpongePostBuildSystemStep(parent: NewProjectWizardStep) : AbstractRunBuildSystemStep(parent, SpongeBuildSystemStep::class.java) {
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
            listOf(BuildRepository(
                "spongepowered-repo",
                "https://repo.spongepowered.org/maven/",
                buildSystems = EnumSet.of(BuildSystemType.MAVEN)
            )),
            listOf(BuildDependency(
                "org.spongepowered",
                "spongeapi",
                spongeApiVersion.toString(),
                mavenScope = "provided"
            ))
        )
    }
}

sealed class SpongeProjectCreator<T : BuildSystem>(
    protected val rootDirectory: Path,
    protected val rootModule: Module,
    protected val buildSystem: T,
    protected val config: SpongeProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

    protected fun setupDependencyStep(): SpongeDependenciesSetup {
        val spongeApiVersion = config.spongeApiVersion
        return SpongeDependenciesSetup(buildSystem, spongeApiVersion, true)
    }

    protected fun setupMainClassSteps(): Pair<CreatorStep, CreatorStep> {
        val mainClassStep = createJavaClassStep(config.mainClass) { packageName, className ->
            SpongeTemplate.applyMainClass(project, packageName, className, config.hasDependencies())
        }

        val (packageName, className) = splitPackage(config.mainClass)
        return mainClassStep to SpongeMainClassModifyStep(project, buildSystem, packageName, className, config)
    }
}

class SpongeMavenCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: MavenBuildSystem,
    config: SpongeProjectConfig
) : SpongeProjectCreator<MavenBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSteps(): Iterable<CreatorStep> {
        val (mainClassStep, modifyStep) = setupMainClassSteps()

        val pomText = SpongeTemplate.applyPom(project, config)

        return listOf(
            setupDependencyStep(),
            BasicMavenStep(project, rootDirectory, buildSystem, config, pomText),
            mainClassStep,
            modifyStep,
            MavenGitignoreStep(project, rootDirectory),
            LicenseStepOld(project, rootDirectory, config.license, config.authors.joinToString(", ")),
            BasicMavenFinalizerStep(rootModule, rootDirectory)
        )
    }
}

class SpongeGradleCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: GradleBuildSystem,
    config: SpongeProjectConfig
) : SpongeProjectCreator<GradleBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    override fun getSteps(): Iterable<CreatorStep> {
        val (mainClassStep, modifyStep) = setupMainClassSteps()

        val buildText = SpongeTemplate.applyBuildGradle(project, buildSystem)
        val propText = SpongeTemplate.applyGradleProp(project)
        val settingsText = SpongeTemplate.applySettingsGradle(project, buildSystem.artifactId)
        val files = GradleFiles(buildText, propText, settingsText)

        return listOf(
            setupDependencyStep(),
            CreateDirectoriesStep(buildSystem, rootDirectory),
            GradleSetupStep(project, rootDirectory, buildSystem, files),
            mainClassStep,
            modifyStep,
            GradleWrapperStepOld(project, rootDirectory, buildSystem),
            GradleGitignoreStep(project, rootDirectory),
            LicenseStepOld(project, rootDirectory, config.license, config.authors.joinToString(", ")),
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem)
        )
    }
}

class SpongeMainClassModifyStep(
    private val project: Project,
    private val buildSystem: BuildSystem,
    private val packageName: String,
    private val className: String,
    private val config: SpongeProjectConfig
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val dirs = buildSystem.dirsOrError

        project.runWriteTaskInSmartMode {
            val targetDir = dirs.sourceDirectory.resolve(Paths.get(packageName.replace('.', '/')))
            if (!Files.isDirectory(targetDir)) {
                throw IllegalStateException("$targetDir is not an existing directory")
            }
            val javaFile = targetDir.resolve("$className.java")
            if (!Files.isRegularFile(javaFile)) {
                throw IllegalStateException("$javaFile is not an existing file")
            }

            val psiFile = PsiManager.getInstance(project).findFile(javaFile.virtualFileOrError) as? PsiJavaFile
                ?: throw IllegalStateException("Failed to resolve PsiJavaFile for $javaFile")
            val psiClass = psiFile.classes[0]

            val annotationString = StringBuilder("@Plugin(")
            annotationString + "\nid = ${escape(buildSystem.artifactId.lowercase(Locale.ENGLISH))}"
            annotationString + ",\nname = ${escape(config.pluginName)}"
            if (buildSystem.type != BuildSystemType.GRADLE) {
                // SpongeGradle will automatically set the Gradle version as plugin version
                annotationString + ",\nversion = ${escape(buildSystem.version)}"
            }

            if (config.hasDescription()) {
                annotationString + ",\ndescription = ${escape(config.description)}"
            }

            if (config.hasWebsite()) {
                annotationString + ",\nurl = ${escape(config.website)}"
            }

            if (config.hasAuthors()) {
                annotationString + ",\nauthors = {\n${config.authors.joinToString(",\n", transform = ::escape)}\n}"
            }

            if (config.hasDependencies()) {
                val dep = config.dependencies.joinToString(",\n") { "@Dependency(id = ${escape(it)})" }
                annotationString + ",\ndependencies = {\n$dep\n}"
            }

            annotationString + "\n)"
            val factory = JavaPsiFacade.getElementFactory(project)
            val annotation = factory.createAnnotationFromText(annotationString.toString(), null)

            psiFile.runWriteAction {
                psiClass.modifierList?.let { modifierList ->
                    modifierList.addBefore(annotation, modifierList.firstChild)
                }
                CodeStyleManager.getInstance(project).reformat(psiClass)
            }
        }
    }

    private fun escape(text: String?): String {
        if (text == null) {
            return "\"\""
        }
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    private operator fun StringBuilder.plus(text: String) = this.append(text)
}

class SpongeDependenciesSetup(
    private val buildSystem: BuildSystem,
    private val spongeApiVersion: String,
    private val addAnnotationProcessor: Boolean
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        buildSystem.repositories.add(
            BuildRepository(
                "spongepowered-repo",
                "https://repo.spongepowered.org/maven/",
                buildSystems = EnumSet.of(BuildSystemType.MAVEN)
            )
        )
        buildSystem.dependencies.add(
            BuildDependency(
                "org.spongepowered",
                "spongeapi",
                spongeApiVersion,
                mavenScope = "provided",
                gradleConfiguration = "compileOnly"
            )
        )
        if (addAnnotationProcessor) {
            buildSystem.dependencies.add(
                BuildDependency(
                    "org.spongepowered",
                    "spongeapi",
                    spongeApiVersion,
                    gradleConfiguration = "annotationProcessor"
                )
            )
        }
    }
}
