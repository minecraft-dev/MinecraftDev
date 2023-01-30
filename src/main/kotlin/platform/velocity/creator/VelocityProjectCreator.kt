/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.AbstractLatentStep
import com.demonwav.mcdev.creator.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.AbstractLongRunningStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AbstractSelectVersionStep
import com.demonwav.mcdev.creator.AuthorsStep
import com.demonwav.mcdev.creator.DependStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.MainClassStep
import com.demonwav.mcdev.creator.PlatformVersion
import com.demonwav.mcdev.creator.PluginNameStep
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
import com.demonwav.mcdev.creator.buildsystem.gradle.GradlePlugin
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.gradle.ReformatBuildGradleStep
import com.demonwav.mcdev.creator.buildsystem.maven.AbstractPatchPomStep
import com.demonwav.mcdev.creator.buildsystem.maven.MavenImportStep
import com.demonwav.mcdev.creator.buildsystem.maven.ReformatPomStep
import com.demonwav.mcdev.creator.chain
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.getVersionSelector
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTaskInSmartMode
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.dsl.builder.Panel
import java.nio.file.Path
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

    override fun createStep(data: PlatformVersion) =
        VelocityVersionStep(this, data.versions.mapNotNull(SemanticVersion::tryParse)).chain(
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

class VelocityVersionStep(
    parent: NewProjectWizardStep,
    versions: List<SemanticVersion>
) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Velocity Version:"

    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        versionProperty.afterChange {
            applyJdkVersion()
        }
        applyJdkVersion()
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
        applyJdkVersion()
    }

    private fun applyJdkVersion() {
        SemanticVersion.tryParse(version)?.let { version ->
            findStep<JdkProjectSetupFinalizer>().preferredJdk = when {
                version >= SemanticVersion.release(3) -> JavaSdkVersion.JDK_11
                else -> JavaSdkVersion.JDK_1_8
            }
        }
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

class VelocityModifyMainClassStep(
    parent: NewProjectWizardStep,
    private val isGradle: Boolean
) : AbstractLongRunningStep(parent) {
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
            val mainClassVirtualFile = VfsUtil.findFile(Path.of(mainClassFile), true)
                ?: return@runWriteTaskInSmartMode
            val mainClassPsi = PsiManager.getInstance(project).findFile(mainClassVirtualFile) as? PsiJavaFile
                ?: return@runWriteTaskInSmartMode

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

class VelocityPostBuildSystemStep(parent: NewProjectWizardStep) : AbstractRunBuildSystemStep(
    parent,
    VelocityBuildSystemStep::class.java
) {
    override val step = BuildSystemSupport.POST_STEP
}

class VelocityGradleSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> VelocityGradleFilesStep(parent).chain(
                ::VelocityPatchGradleFilesStep,
                ::GradleWrapperStep
            )
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent).chain(
                ::ReformatBuildGradleStep,
                { VelocityModifyMainClassStep(it, true) }
            )
            else -> EmptyStep(parent)
        }
    }
}

class VelocityGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val projectName = baseData.name
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
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

        val buildConstantsJava = "src/main/java/${mainPackage.replace('.', '/')}/BuildConstants.java"
        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.VELOCITY_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.VELOCITY_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.VELOCITY_SETTINGS_GRADLE_TEMPLATE,
            buildConstantsJava to MinecraftTemplates.VELOCITY_BUILD_CONSTANTS_TEMPLATE,
        )
    }
}

class VelocityPatchGradleFilesStep(parent: NewProjectWizardStep) : AbstractPatchGradleFilesStep(parent) {
    override fun patch(project: Project, gradleFiles: GradleFiles) {
        val velocityApiVersion = data.getUserData(VelocityVersionStep.KEY) ?: return

        addPlugins(
            project, gradleFiles.buildGradle,
            listOf(
                GradlePlugin("org.jetbrains.gradle.plugin.idea-ext", "1.0.1")
            )
        )
        addRepositories(
            project, gradleFiles.buildGradle,
            listOf(
                BuildRepository(
                    "papermc-repo",
                    "https://repo.papermc.io/repository/maven-public/"
                )
            )
        )
        val annotationArtifactId =
            if (velocityApiVersion >= VelocityConstants.API_4) "velocity-annotation-processor" else "velocity-api"
        addDependencies(
            project, gradleFiles.buildGradle,
            listOf(
                BuildDependency(
                    "com.velocitypowered",
                    "velocity-api",
                    velocityApiVersion.toString(),
                    gradleConfiguration = "compileOnly"
                ),
                BuildDependency(
                    "com.velocitypowered",
                    annotationArtifactId,
                    velocityApiVersion.toString(),
                    gradleConfiguration = "annotationProcessor"
                ),
            )
        )
    }
}

class VelocityMavenSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> VelocityMavenFilesStep(parent).chain(::VelocityPatchPomStep)
            BuildSystemSupport.POST_STEP -> MavenImportStep(parent).chain(
                ::ReformatPomStep,
                { VelocityModifyMainClassStep(it, false) }
            )
            else -> EmptyStep(parent)
        }
    }
}

class VelocityMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
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

        val annotationArtifactId =
            if (velocityApiVersion >= VelocityConstants.API_4) "velocity-annotation-processor" else "velocity-api"
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
                    annotationArtifactId,
                    velocityApiVersion.toString(),
                    mavenScope = if (velocityApiVersion >= VelocityConstants.API_4) "provided" else null,
                )
            )
        )
    }
}
