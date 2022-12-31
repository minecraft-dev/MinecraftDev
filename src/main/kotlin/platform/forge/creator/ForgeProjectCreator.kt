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

import com.demonwav.mcdev.creator.*
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.gradle.*
import com.demonwav.mcdev.creator.platformtype.ModPlatformStep
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.forge.util.ForgePackAdditionalData
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.*
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.lang.JavaVersion
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.coroutineScope

private val minSupportedMcVersion = MinecraftVersions.MC1_16_5

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
            ::ForgeGradleFilesStep,
            ::GradleWrapperStep,
            ::ForgeProjectFilesStep,
            ::ForgeMixinsJsonStep,
            ::ForgeCompileJavaStep,
            ::GradleImportStep,
        )

    class Factory : ModPlatformStep.Factory {
        override val name = "Forge"
        override fun createStep(parent: ModPlatformStep) = ForgePlatformStep(parent)
    }
}

class ForgeMcVersionStep(parent: NewProjectWizardStep, private val forgeVersionData: ForgeVersion) : AbstractSelectVersionThenForkStep<SemanticVersion>(parent, forgeVersionData.sortedMcVersions.filter { it >= minSupportedMcVersion }) {
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

class ForgeVersionStep(parent: NewProjectWizardStep, versions: List<SemanticVersion>) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
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

class ForgeGradleFilesStep(parent: NewProjectWizardStep) : FixedAssetsNewProjectWizardStep(parent) {
    private fun transformModName(modName: String): String {
        return modName.lowercase(Locale.ENGLISH).replace(" ", "")
    }

    override fun setupAssets(project: Project) {
        outputDirectory = context.projectFileDirectory

        val mcVersion = data.getUserData(ForgeMcVersionStep.KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionStep.KEY) ?: return
        val modName = transformModName(data.getUserData(ModNameStep.KEY) ?: return)
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val javaVersion = context.projectJdk.versionString?.let(JavaVersion::parse)
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false

        addTemplateProperties(
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
            addTemplateProperties("JAVA_VERSION" to javaVersion.feature)
        }

        if (authors.isNotEmpty()) {
            addTemplateProperties("AUTHOR_LIST" to authors.joinToString(", "))
        }

        if (useMixins) {
            addTemplateProperties("MIXINS" to "true")
        }

        if (forgeVersion >= SemanticVersion.release(39, 0, 88)) {
            addTemplateProperties("GAME_TEST_FRAMEWORK" to "true")
        }

        if (mcVersion <= MinecraftVersions.MC1_16_5) {
            addTemplateProperties(
                "MCP_CHANNEL" to "snapshot",
                "MCP_VERSION" to "20210309",
            )
        }

        addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.FG3_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.FG3_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.FG3_SETTINGS_GRADLE_TEMPLATE,
        )

        addGradleWrapperProperties(project)
    }
}

class ForgeProjectFilesStep(parent: NewProjectWizardStep) : FixedAssetsNewProjectWizardStep(parent) {
    override fun setupAssets(project: Project) {
        outputDirectory = context.projectFileDirectory

        val mcVersion = data.getUserData(ForgeMcVersionStep.KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionStep.KEY) ?: return
        val (mainPackageName, mainClassName) = splitPackage(data.getUserData(MainClassStep.KEY) ?: return)
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modName = data.getUserData(ModNameStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val updateUrl = data.getUserData(UpdateUrlStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()

        val nextMcVersion = when (val part = mcVersion.parts.getOrNull(1)) {
            // Mimics the code used to get the next Minecraft version in Forge's MDK
            // https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/build.gradle#L884
            is SemanticVersion.Companion.VersionPart.ReleasePart -> (part.version + 1).toString()
            null -> "?"
            else -> part.versionString
        }

        val packDescriptor = ForgePackDescriptor.forMcVersion(mcVersion) ?: ForgePackDescriptor.FORMAT_3
        val additionalPackData = ForgePackAdditionalData.forMcVersion(mcVersion)

        addTemplateProperties(
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
            "YEAR" to ZonedDateTime.now().year,
        )

        if (updateUrl.isNotBlank()) {
            addTemplateProperties("UPDATE_URL" to updateUrl)
        }

        if (authors.isNotEmpty()) {
            addTemplateProperties("AUTHOR_LIST" to authors.joinToString(", "))
        }
        addTemplateProperties("AUTHOR" to authors.joinToString(", "))

        val mainClassTemplate = when {
            mcVersion >= MinecraftVersions.MC1_19_3 -> MinecraftTemplates.FG3_1_19_3_MAIN_CLASS_TEMPLATE
            mcVersion >= MinecraftVersions.MC1_19 -> MinecraftTemplates.FG3_1_19_MAIN_CLASS_TEMPLATE
            mcVersion >= MinecraftVersions.MC1_18 -> MinecraftTemplates.FG3_1_18_MAIN_CLASS_TEMPLATE
            mcVersion >= MinecraftVersions.MC1_17 -> MinecraftTemplates.FG3_1_17_MAIN_CLASS_TEMPLATE
            else -> MinecraftTemplates.FG3_MAIN_CLASS_TEMPLATE
        }

        addTemplates(
            project,
            "src/main/java/${mainPackageName.replace('.', '/')}/$mainClassName.java" to mainClassTemplate,
            "src/main/resources/pack.mcmeta" to MinecraftTemplates.PACK_MCMETA_TEMPLATE,
            "src/main/resources/META-INF/mods.toml" to MinecraftTemplates.MODS_TOML_TEMPLATE,
            "LICENSE" to "${license.id}.txt",
            ".gitignore" to MinecraftTemplates.GRADLE_GITIGNORE_TEMPLATE,
        )

        WriteAction.runAndWait<Throwable> {
            val dir = VfsUtil.createDirectoryIfMissing(LocalFileSystem.getInstance(), "$outputDirectory/.gradle")
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
class ForgeMixinsJsonStep(parent: NewProjectWizardStep) : FixedAssetsNewProjectWizardStep(parent) {
    override fun setupAssets(project: Project) {
        outputDirectory = context.projectFileDirectory

        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false
        if (useMixins) {
            val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
            addTemplateProperties(
                "PACKAGE_NAME" to "${buildSystemProps.groupId}.${buildSystemProps.artifactId}.mixin",
                "ARTIFACT_ID" to buildSystemProps.artifactId,
            )
            addTemplates(project, "src/main/resources/${buildSystemProps.artifactId}.mixins.json" to MinecraftTemplates.FORGE_MIXINS_JSON_TEMPLATE)
        }
    }
}

class ForgeCompileJavaStep(parent: NewProjectWizardStep) : AbstractRunGradleTaskStep(parent) {
    override val title = "Setting up classpath"
    override val task = "compileJava"
}

class Fg2ProjectCreator(
    private val rootDirectory: Path,
    private val rootModule: Module,
    private val buildSystem: GradleBuildSystem,
    private val config: ForgeProjectConfig,
    private val mcVersion: SemanticVersion
) : BaseProjectCreator(rootModule, buildSystem) {

    private fun setupMainClassStep(): BasicJavaClassStep {
        return createJavaClassStep(config.mainClass) { packageName, className ->
            Fg2Template.applyMainClass(project, buildSystem, config, packageName, className)
        }
    }

    override fun getSteps(): Iterable<CreatorStep> {
        val buildText = Fg2Template.applyBuildGradle(project, buildSystem, mcVersion)
        val propText = Fg2Template.applyGradleProp(project, config)
        val settingsText = Fg2Template.applySettingsGradle(project, buildSystem.artifactId)
        val files = GradleFiles(buildText, propText, settingsText)

        return listOf(
            SimpleGradleSetupStep(
                project,
                rootDirectory,
                buildSystem,
                files
            ),
            setupMainClassStep(),
            GradleWrapperStepOld(project, rootDirectory, buildSystem),
            McmodInfoStep(project, buildSystem, config),
            SetupDecompWorkspaceStep(project, rootDirectory),
            GradleGitignoreStep(project, rootDirectory),
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem),
            ForgeRunConfigsStep(buildSystem, rootDirectory, config, CreatedModuleType.SINGLE)
        )
    }

    companion object {
        val FG_WRAPPER_VERSION = SemanticVersion.release(4, 10, 3)
    }
}

open class Fg3ProjectCreator(
    protected val rootDirectory: Path,
    protected val rootModule: Module,
    protected val buildSystem: GradleBuildSystem,
    protected val config: ForgeProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

    private fun setupMainClassStep(): BasicJavaClassStep {
        return createJavaClassStep(config.mainClass) { packageName, className ->
            if (config.mcVersion >= MinecraftVersions.MC1_19_3) {
                Fg3Template.apply1_19_3MainClass(project, buildSystem, config, packageName, className)
            } else if (config.mcVersion >= MinecraftVersions.MC1_19) {
                Fg3Template.apply1_19MainClass(project, buildSystem, config, packageName, className)
            } else if (config.mcVersion >= MinecraftVersions.MC1_18) {
                Fg3Template.apply1_18MainClass(project, buildSystem, config, packageName, className)
            } else if (config.mcVersion >= MinecraftVersions.MC1_17) {
                Fg3Template.apply1_17MainClass(project, buildSystem, config, packageName, className)
            } else {
                Fg3Template.applyMainClass(project, buildSystem, config, packageName, className)
            }
        }
    }

    protected fun transformModName(modName: String?): String {
        modName ?: return "examplemod"
        return modName.lowercase(Locale.ENGLISH).replace(" ", "")
    }

    protected fun createGradleFiles(hasData: Boolean): GradleFiles<String> {
        val modName = transformModName(config.pluginName)
        val buildText = Fg3Template.applyBuildGradle(project, buildSystem, config, modName, hasData)
        val propText = Fg3Template.applyGradleProp(project)
        val settingsText = Fg3Template.applySettingsGradle(project, buildSystem.artifactId)
        return GradleFiles(buildText, propText, settingsText)
    }

    override fun getSteps(): Iterable<CreatorStep> {
        val files = createGradleFiles(hasData = true)
        val steps = mutableListOf(
            SimpleGradleSetupStep(
                project,
                rootDirectory,
                buildSystem,
                files
            ),
            setupMainClassStep(),
            GradleWrapperStepOld(project, rootDirectory, buildSystem),
            Fg3ProjectFilesStep(project, buildSystem, config),
            Fg3CompileJavaStep(project, rootDirectory),
            GradleGitignoreStep(project, rootDirectory),
            LicenseStepOld(project, rootDirectory, config.license, config.authors.joinToString(", ")),
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem),
            ForgeRunConfigsStep(buildSystem, rootDirectory, config, CreatedModuleType.SINGLE)
        )

        if (config.mixins) {
            steps += MixinConfigStep(project, buildSystem)
        }

        return steps
    }

    companion object {
        val FG5_WRAPPER_VERSION = SemanticVersion.release(7, 4, 2)
    }
}

class Fg3Mc112ProjectCreator(
    rootDirectory: Path,
    rootModule: Module,
    buildSystem: GradleBuildSystem,
    config: ForgeProjectConfig
) : Fg3ProjectCreator(rootDirectory, rootModule, buildSystem, config) {

    private fun setupMainClassStep(): BasicJavaClassStep {
        return createJavaClassStep(config.mainClass) { packageName, className ->
            Fg2Template.applyMainClass(project, buildSystem, config, packageName, className)
        }
    }

    override fun getSteps(): Iterable<CreatorStep> {
        val files = createGradleFiles(hasData = false)

        return listOf(
            SimpleGradleSetupStep(
                project,
                rootDirectory,
                buildSystem,
                files
            ),
            setupMainClassStep(),
            GradleWrapperStepOld(project, rootDirectory, buildSystem),
            McmodInfoStep(project, buildSystem, config),
            Fg3CompileJavaStep(project, rootDirectory),
            GradleGitignoreStep(project, rootDirectory),
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem),
            ForgeRunConfigsStep(buildSystem, rootDirectory, config, CreatedModuleType.SINGLE)
        )
    }
}

class SetupDecompWorkspaceStep(
    private val project: Project,
    private val rootDirectory: Path
) : CreatorStep {

    override fun runStep(indicator: ProgressIndicator) {
        indicator.text = "Setting up project"
        indicator.text2 = "Running Gradle task: 'setupDecompWorkspace'"
        runGradleTaskAndWait(project, rootDirectory) { settings ->
            settings.taskNames = listOf("setupDecompWorkspace")
            settings.vmOptions = "-Xmx2G"
        }
        indicator.text2 = null
    }
}

class McmodInfoStep(
    private val project: Project,
    private val buildSystem: BuildSystem,
    private val config: ForgeProjectConfig
) : CreatorStep {

    override fun runStep(indicator: ProgressIndicator) {
        val text = Fg2Template.applyMcmodInfo(project, buildSystem, config)
        val dir = buildSystem.dirsOrError.resourceDirectory
        runWriteTask {
            CreatorStep.writeTextToFile(project, dir, ForgeConstants.MCMOD_INFO, text)
        }
    }
}

class Fg3ProjectFilesStep(
    private val project: Project,
    private val buildSystem: BuildSystem,
    private val config: ForgeProjectConfig
) : CreatorStep {

    override fun runStep(indicator: ProgressIndicator) {
        val modsTomlText = Fg3Template.applyModsToml(project, buildSystem, config)
        val packDescriptor = ForgePackDescriptor.forMcVersion(config.mcVersion) ?: ForgePackDescriptor.FORMAT_3
        val additionalData = ForgePackAdditionalData.forMcVersion(config.mcVersion)
        val packMcmetaText =
            Fg3Template.applyPackMcmeta(project, buildSystem.artifactId, packDescriptor, additionalData)
        val dir = buildSystem.dirsOrError.resourceDirectory
        runWriteTask {
            CreatorStep.writeTextToFile(project, dir, ForgeConstants.PACK_MCMETA, packMcmetaText)
            val meta = dir.resolve("META-INF")
            Files.createDirectories(meta)
            CreatorStep.writeTextToFile(project, meta, ForgeConstants.MODS_TOML, modsTomlText)
        }
    }
}

class Fg3CompileJavaStep(
    private val project: Project,
    private val rootDirectory: Path
) : CreatorStep {

    override fun runStep(indicator: ProgressIndicator) {
        indicator.text = "Setting up classpath"
        indicator.text2 = "Running Gradle task: 'compileJava'"
        runGradleTaskAndWait(project, rootDirectory) { settings ->
            settings.taskNames = listOf("compileJava")
        }
        indicator.text2 = null
    }
}

class MixinConfigStep(
    private val project: Project,
    private val buildSystem: BuildSystem
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val text = Fg3Template.applyMixinConfigTemplate(project, buildSystem)
        val dir = buildSystem.dirsOrError.resourceDirectory
        runWriteTask {
            CreatorStep.writeTextToFile(project, dir, "${buildSystem.artifactId}.mixins.json", text)
        }
    }
}

enum class CreatedModuleType {
    SINGLE, MULTI
}

class ForgeRunConfigsStep(
    private val buildSystem: BuildSystem,
    private val rootDirectory: Path,
    private val config: ForgeProjectConfig,
    private val createdModuleType: CreatedModuleType
) : CreatorStep {

    override fun runStep(indicator: ProgressIndicator) {
        val gradleDir = rootDirectory.resolve(".gradle")
        Files.createDirectories(gradleDir)
        val hello = gradleDir.resolve(MAGIC_RUN_CONFIGS_FILE)

        val task = if (createdModuleType == CreatedModuleType.MULTI) {
            ":${buildSystem.artifactId}:genIntellijRuns"
        } else {
            "genIntellijRuns"
        }

        // We don't use `rootModule.name` here because Gradle will change the name of the module to match
        // what was set as the artifactId once it imports the project
        val moduleName = if (createdModuleType == CreatedModuleType.MULTI) {
            "${buildSystem.parentOrError.artifactId}.${buildSystem.artifactId}"
        } else {
            buildSystem.artifactId
        }

        val fileContents = moduleName + "\n" +
            config.mcVersion + "\n" +
            config.forgeVersion + "\n" +
            task

        Files.write(hello, fileContents.toByteArray(Charsets.UTF_8), CREATE, TRUNCATE_EXISTING, WRITE)
    }
}

const val MAGIC_RUN_CONFIGS_FILE = ".hello_from_mcdev"
