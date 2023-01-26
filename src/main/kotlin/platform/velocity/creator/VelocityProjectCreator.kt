/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.*
import com.demonwav.mcdev.creator.buildsystem.*
import com.demonwav.mcdev.creator.buildsystem.gradle.*
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.*
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.*
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlTag
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.coroutineScope
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel

class VelocityPlatformStep(parent: PluginPlatformStep) : AbstractLatentStep<PlatformVersion>(parent) {
    override val description = "download Velocity versions"

    override suspend fun computeData() = coroutineScope {
        try {
            asyncIO { getVersionSelector(PlatformType.VELOCITY) }.await()
        } catch (e: Throwable) {
            null
        }
    }

    override fun createStep(data: PlatformVersion) = VelocityVersionStep(this, data.versions.mapNotNull(SemanticVersion::tryParse)).chain(
        ::PluginNameStep,
        ::MainClassStep,
        ::VelocityOptionalSettingsStep,
        ::VelocityBuildSystemStep,
        ::VelocityProjectFilesStep,
        ::VelocityPostBuildSystemStep,
    )

    class Factory : PluginPlatformStep.Factory {
        override val name = "Velocity"

        override fun createStep(parent: PluginPlatformStep) = VelocityPlatformStep(parent)
    }
}

class VelocityVersionStep(parent: NewProjectWizardStep, versions: List<SemanticVersion>) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Velocity Version:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${VelocityVersionStep::class.java.name}.version")
    }
}

class VelocityOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(
        ::AuthorsStep,
        ::WebsiteStep,
        ::DependStep,
    )
}

class VelocityProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating project files"

    override fun setupAssets(project: Project) {
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val dependencies = data.getUserData(DependStep.KEY) ?: emptyList()
        val (packageName, className) = splitPackage(mainClass)
        val version = data.getUserData(VelocityVersionStep.KEY) ?: return

        assets.addTemplateProperties(
            "PACKAGE" to packageName,
            "CLASS_NAME" to className,
        )

        if (dependencies.isNotEmpty()) {
            assets.addTemplateProperties(
                "HAS_DEPENDENCIES" to "true",
            )
        }

        val template = if (version < VelocityConstants.API_2 ||
            (version >= VelocityConstants.API_3 && version < VelocityConstants.API_4)
        ) {
            MinecraftTemplates.VELOCITY_MAIN_CLASS_TEMPLATE // API 1 and 3
        } else {
            MinecraftTemplates.VELOCITY_MAIN_CLASS_V2_TEMPLATE // API 2 and 4 (4+ maybe ?)
        }

        assets.addTemplates(
            project,
            "src/main/java/${mainClass.replace('.', '/')}.java" to template
        )
    }
}

class VelocityModifyMainClassStep(parent: NewProjectWizardStep, private val isGradle: Boolean) : AbstractLongRunningStep(parent) {
    override val description = "Patching main class"

    override fun perform(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mainClassName = data.getUserData(MainClassStep.KEY) ?: return
        val mainClassFile = "${context.projectFileDirectory}/src/main/java/${mainClassName.replace('.', '/')}.java"
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val dependencies = data.getUserData(DependStep.KEY) ?: emptyList()

        project.runWriteTaskInSmartMode {
            val mainClassVirtualFile = VfsUtil.findFile(Path.of(mainClassFile), true) ?: return@runWriteTaskInSmartMode
            val mainClassPsi = PsiManager.getInstance(project).findFile(mainClassVirtualFile) as? PsiJavaFile ?: return@runWriteTaskInSmartMode

            val psiClass = mainClassPsi.classes[0]
            val annotation = buildString {
                append("@Plugin(")
                append("\nid = ${literal(buildSystemProps.artifactId)}")
                append(",\nname = ${literal(pluginName)}")

                if (isGradle) {
                    append(",\nversion = BuildConstants.VERSION")
                } else {
                    append(",\nversion = \"${buildSystemProps.version}\"")
                }

                if (description.isNotBlank()) {
                    append(",\ndescription = ${literal(description)}")
                }

                if (website.isNotBlank()) {
                    append(",\nurl = ${literal(website)}")
                }

                if (authors.isNotEmpty()) {
                    append(",\nauthors = {${authors.joinToString(", ", transform = ::literal)}}")
                }

                if (dependencies.isNotEmpty()) {
                    val deps = dependencies.joinToString(",\n") { "@Dependency(id = ${literal(it)})" }
                    append(",\ndependencies = {\n$deps\n}")
                }

                append("\n)")
            }

            val factory = JavaPsiFacade.getElementFactory(project)
            val pluginAnnotation = factory.createAnnotationFromText(annotation, null)

            mainClassPsi.runWriteAction {
                psiClass.modifierList?.let { it.addBefore(pluginAnnotation, it.firstChild) }
                CodeStyleManager.getInstance(project).reformat(psiClass)
            }
        }
    }

    private fun literal(text: String?): String {
        if (text == null) {
            return "\"\""
        }
        return '"' + StringUtil.escapeStringCharacters(text) + '"'
    }
}

class VelocityBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Velocity"
}

class VelocityPostBuildSystemStep(parent: NewProjectWizardStep) : AbstractRunBuildSystemStep(parent, VelocityBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}

class VelocityGradleSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> VelocityGradleFilesStep(parent).chain(::VelocityPatchGradleFilesStep, ::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent).chain(::ReformatBuildGradleStep, { VelocityModifyMainClassStep(it, true) })
            else -> EmptyStep(parent)
        }
    }
}

class VelocityGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val projectName = baseData.name
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().minVersion.ordinal
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val (mainPackage, _) = splitPackage(mainClass)

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "PLUGIN_ID" to buildSystemProps.artifactId,
            "PLUGIN_VERSION" to buildSystemProps.version,
            "JAVA_VERSION" to javaVersion,
            "PROJECT_NAME" to projectName,
            "PACKAGE" to mainPackage,
        )

        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.VELOCITY_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.VELOCITY_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.VELOCITY_SETTINGS_GRADLE_TEMPLATE,
            "src/main/java/${mainPackage.replace('.', '/')}/BuildConstants.java" to MinecraftTemplates.VELOCITY_BUILD_CONSTANTS_TEMPLATE,
        )
    }
}

class VelocityPatchGradleFilesStep(parent: NewProjectWizardStep) : AbstractPatchGradleFilesStep(parent) {
    override fun patch(project: Project, gradleFiles: GradleFiles) {
        val velocityApiVersion = data.getUserData(VelocityVersionStep.KEY) ?: return

        addPlugins(project, gradleFiles.buildGradle, listOf(
            GradlePlugin("org.jetbrains.gradle.plugin.idea-ext", "1.0.1")
        ))
        addRepositories(project, gradleFiles.buildGradle, listOf(
            BuildRepository(
                "papermc-repo",
                "https://repo.papermc.io/repository/maven-public/"
            )
        ))
        addDependencies(project, gradleFiles.buildGradle, listOf(
            BuildDependency(
                "com.velocitypowered",
                "velocity-api",
                velocityApiVersion.toString(),
                gradleConfiguration = "compileOnly"
            ),
            BuildDependency(
                "com.velocitypowered",
                if (velocityApiVersion >= VelocityConstants.API_4) "velocity-annotation-processor" else "velocity-api",
                velocityApiVersion.toString(),
                gradleConfiguration = "annotationProcessor"
            ),
        ))
    }
}

class VelocityMavenSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> VelocityMavenFilesStep(parent).chain(::VelocityPatchPomStep)
            BuildSystemSupport.POST_STEP -> MavenImportStep(parent).chain(::ReformatPomStep, { VelocityModifyMainClassStep(it, false) })
            else -> EmptyStep(parent)
        }
    }
}

class VelocityMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        val javaVersion = findStep<JdkProjectSetupFinalizer>().minVersion.ordinal
        assets.addTemplateProperties(
            "JAVA_VERSION" to javaVersion,
        )
        assets.addTemplates(
            project,
            "pom.xml" to MinecraftTemplates.VELOCITY_POM_TEMPLATE,
        )
    }
}

class VelocityPatchPomStep(parent: NewProjectWizardStep) : AbstractPatchPomStep(parent) {
    override fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        super.patchPom(model, root)

        val velocityApiVersion = data.getUserData(VelocityVersionStep.KEY) ?: return

        setupDependencies(
            model,
            listOf(
                BuildRepository(
                    "papermc-repo",
                    "https://repo.papermc.io/repository/maven-public/"
                )
            ),
            listOf(
                BuildDependency(
                    "com.velocitypowered",
                    "velocity-api",
                    velocityApiVersion.toString(),
                    mavenScope = "provided",
                ),
                BuildDependency(
                    "com.velocitypowered",
                    if (velocityApiVersion >= VelocityConstants.API_4) "velocity-annotation-processor" else "velocity-api",
                    velocityApiVersion.toString(),
                    mavenScope = if (velocityApiVersion >= VelocityConstants.API_4) "provided" else null,
                )
            )
        )
    }
}

sealed class VelocityProjectCreator<T : BuildSystem>(
    protected val rootDirectory: Path,
    protected val rootModule: Module,
    protected val buildSystem: T,
    protected val config: VelocityProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

    protected fun setupDependencyStep(): VelocityDependenciesSetup {
        val velocityApiVersion = config.velocityApiVersion
        return VelocityDependenciesSetup(buildSystem, velocityApiVersion)
    }

    protected fun setupMainClassSteps(): Pair<CreatorStep, CreatorStep> {
        val mainClassStep = createJavaClassStep(config.mainClass) { packageName, className ->
            val version = config.apiVersion
            VelocityTemplate.applyMainClass(project, packageName, className, config.hasDependencies(), version)
        }

        return mainClassStep to VelocityMainClassModifyStep(project, buildSystem, config.mainClass, config)
    }
}

class VelocityMavenCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: MavenBuildSystem,
    config: VelocityProjectConfig
) : VelocityProjectCreator<MavenBuildSystem>(rootDirectory, rootModule, buildSystem, config) {
    override fun getSteps(): Iterable<CreatorStep> {
        val (mainClassStep, modifyStep) = setupMainClassSteps()

        val pomText = VelocityTemplate.applyPom(project, config)

        return listOf(
            setupDependencyStep(),
            BasicMavenStep(project, rootDirectory, buildSystem, config, pomText),
            mainClassStep,
            modifyStep,
            MavenGitignoreStep(project, rootDirectory),
            BasicMavenFinalizerStep(rootModule, rootDirectory)
        )
    }
}

class VelocityGradleCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: GradleBuildSystem,
    config: VelocityProjectConfig
) : VelocityProjectCreator<GradleBuildSystem>(rootDirectory, rootModule, buildSystem, config) {

    private val ideaExtPlugin = GradlePlugin("org.jetbrains.gradle.plugin.idea-ext", "1.0.1")

    override fun getSteps(): Iterable<CreatorStep> {
        val (mainClassStep, modifyStep) = setupMainClassSteps()

        val buildText = VelocityTemplate.applyBuildGradle(project, buildSystem, config)
        val propText = VelocityTemplate.applyGradleProp(project, null)
        val settingsText = VelocityTemplate.applySettingsGradle(project, buildSystem.artifactId)
        val files = GradleFiles(buildText, propText, settingsText)

        return listOf(
            setupDependencyStep(),
            CreateDirectoriesStep(buildSystem, rootDirectory),
            GradleSetupStep(project, rootDirectory, buildSystem, files),
            AddGradlePluginStep(project, rootDirectory, listOf(ideaExtPlugin)),
            mainClassStep,
            modifyStep,
            buildConstantsStep(),
            GradleWrapperStepOld(project, rootDirectory, buildSystem),
            GradleGitignoreStep(project, rootDirectory),
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem)
        )
    }

    private fun buildConstantsStep(): BasicJavaClassStep {
        return BasicJavaClassStep(
            project,
            buildSystem,
            config.mainClass.replaceAfterLast('.', "BuildConstants"),
            VelocityTemplate.applyBuildConstants(project, config.mainClass.substringBeforeLast('.')),
            false
        ) { it.dirsOrError.sourceDirectory.resolveSibling("templates") }
    }
}

class VelocityMainClassModifyStep(
    private val project: Project,
    private val buildSystem: BuildSystem,
    private val classFullName: String,
    private val config: VelocityProjectConfig
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val dirs = buildSystem.dirsOrError

        project.runWriteTaskInSmartMode {
            val classFile = dirs.sourceDirectory.resolve(Paths.get(classFullName.replace('.', '/') + ".java"))
            if (!Files.isRegularFile(classFile)) {
                throw IllegalStateException("$classFile is not an existing file")
            }

            val psiFile = PsiManager.getInstance(project).findFile(classFile.virtualFileOrError) as? PsiJavaFile
                ?: throw IllegalStateException("Failed to resolve PsiJavaFile for $classFile")
            val psiClass = psiFile.classes[0]
            val annotationBuilder = StringBuilder("@Plugin(")
            annotationBuilder + "\nid = ${literal(buildSystem.artifactId)}"
            annotationBuilder + ",\nname = ${literal(config.pluginName)}"

            if (buildSystem is GradleBuildSystem) {
                annotationBuilder + ",\nversion = BuildConstants.VERSION"
            } else {
                annotationBuilder + ",\nversion = \"${buildSystem.version}\""
            }

            if (config.hasDescription()) {
                annotationBuilder + ",\ndescription = ${literal(config.description)}"
            }

            if (config.hasWebsite()) {
                annotationBuilder + ",\nurl = ${literal(config.website)}"
            }

            if (config.hasAuthors()) {
                annotationBuilder + ",\nauthors = {${config.authors.joinToString(", ", transform = ::literal)}}"
            }

            if (config.hasDependencies()) {
                val deps = config.dependencies.joinToString(",\n") { "@Dependency(id = ${literal(it)})" }
                annotationBuilder + ",\ndependencies = {\n$deps\n}"
            }

            annotationBuilder + "\n)"
            val factory = JavaPsiFacade.getElementFactory(project)
            val pluginAnnotation = factory.createAnnotationFromText(annotationBuilder.toString(), null)

            psiFile.runWriteAction {
                psiClass.modifierList?.let { it.addBefore(pluginAnnotation, it.firstChild) }
                CodeStyleManager.getInstance(project).reformat(psiClass)
            }
        }
    }

    private fun literal(text: String?): String {
        if (text == null) {
            return "\"\""
        }
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"'
    }

    private operator fun StringBuilder.plus(text: String) = this.append(text)
}

class VelocityDependenciesSetup(
    private val buildSystem: BuildSystem,
    private val velocityApiVersion: String
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        buildSystem.repositories.add(
            BuildRepository(
                "papermc-repo",
                "https://repo.papermc.io/repository/maven-public/"
            )
        )

        buildSystem.dependencies.add(
            BuildDependency(
                "com.velocitypowered",
                "velocity-api",
                velocityApiVersion,
                mavenScope = "provided",
                gradleConfiguration = "compileOnly"
            )
        )
        val semanticApiVersion = SemanticVersion.parse(velocityApiVersion)
        buildSystem.dependencies.add(
            BuildDependency(
                "com.velocitypowered",
                if (semanticApiVersion >= VelocityConstants.API_4) "velocity-annotation-processor" else "velocity-api",
                velocityApiVersion,
                mavenScope = if (semanticApiVersion >= VelocityConstants.API_4) "provided" else null,
                gradleConfiguration = "annotationProcessor"
            )
        )
    }
}
