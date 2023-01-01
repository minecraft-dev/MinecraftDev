/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.creator

import com.demonwav.mcdev.creator.*
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.gradle.*
import com.demonwav.mcdev.creator.platformtype.ModPlatformStep
import com.demonwav.mcdev.platform.architectury.version.ArchitecturyVersion
import com.demonwav.mcdev.platform.fabric.EntryPoint
import com.demonwav.mcdev.platform.fabric.creator.FabricApiVersionStep
import com.demonwav.mcdev.platform.fabric.creator.FabricLoaderVersionStep
import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.platform.forge.creator.ForgeVersionStep
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.forge.util.ForgePackAdditionalData
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.*
import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.coroutineScope
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils

class ArchitecturyVersionData(
    val forgeVersions: ForgeVersion,
    val fabricVersions: FabricVersions,
    val fabricApiVersions: FabricApiVersions,
    val architecturyVersions: ArchitecturyVersion,
)

private val NewProjectWizardStep.architecturyGroup: String get() {
    val apiVersion = data.getUserData(ArchitecturyApiVersionStep.KEY)
    return when {
        apiVersion == null || apiVersion >= SemanticVersion.release(2, 0, 10) -> "dev.architectury"
        else -> "me.shedaniel"
    }
}

private val NewProjectWizardStep.architecturyPackage: String get() {
    val apiVersion = data.getUserData(ArchitecturyApiVersionStep.KEY)
    return when {
        apiVersion == null || apiVersion >= SemanticVersion.release(2, 0, 10) -> "dev.architectury"
        else -> "me.shedaniel.architectury"
    }
}

class ArchitecturyPlatformStep(parent: ModPlatformStep) : AbstractLatentStep<ArchitecturyVersionData>(parent) {
    override val description = "download Forge, Fabric and Architectury versions"

    override suspend fun computeData() = coroutineScope {
        val forgeJob = asyncIO { ForgeVersion.downloadData() }
        val fabricJob = asyncIO { FabricVersions.downloadData() }
        val fabricApiJob = asyncIO { FabricApiVersions.downloadData() }
        val archJob = asyncIO { ArchitecturyVersion.downloadData() }

        val forge = forgeJob.await() ?: return@coroutineScope null
        val fabric = fabricJob.await() ?: return@coroutineScope null
        val fabricApi = fabricApiJob.await() ?: return@coroutineScope null
        val arch = archJob.await() ?: return@coroutineScope null

        ArchitecturyVersionData(forge, fabric, fabricApi, arch)
    }

    override fun createStep(data: ArchitecturyVersionData): NewProjectWizardStep {
        val mcVersions = data.architecturyVersions.versions.keys.intersect(data.forgeVersions.sortedMcVersions.toSet())
            .intersect(data.fabricVersions.game.mapNotNullTo(mutableSetOf()) { SemanticVersion.tryParse(it.version) })
        return ArchitecturyMcVersionStep(this, mcVersions.toList(), data).chain(
            ::UseMixinsStep,
            ::ModNameStep,
            ::LicenseStep,
            ::ArchitecturyOptionalSettingsStep,
            ::ArchitecturyGradleFilesStep,
            ::GradleWrapperStep,
            ::GradleImportStep,
        )
    }

    class Factory : ModPlatformStep.Factory {
        override val name = "Architectury"
        override fun createStep(parent: ModPlatformStep) = ArchitecturyPlatformStep(parent)
    }
}

class ArchitecturyMcVersionStep(parent: NewProjectWizardStep, mcVersions: List<SemanticVersion>, private val versionData: ArchitecturyVersionData) : AbstractSelectVersionThenForkStep<SemanticVersion>(parent, mcVersions) {
    override val label = "Minecraft Version:"

    override fun initStep(version: SemanticVersion) = ForgeVersionStep(this, versionData.forgeVersions.getForgeVersions(version)).chain(
        { parent -> FabricLoaderVersionStep(parent, versionData.fabricVersions.loader) },
        { parent ->
            val versionStr = version.toString()
            val apiVersions = versionData.fabricApiVersions.versions.filter { versionStr in it.gameVersions }.map { it.version }
            if (apiVersions.isEmpty()) {
                FabricApiVersionStep(parent, versionData.fabricApiVersions.versions.map { it.version }, false)
            } else {
                FabricApiVersionStep(parent, apiVersions, true)
            }
        },
        { parent -> ArchitecturyApiVersionStep(parent, versionData.architecturyVersions.getArchitecturyVersions(version)) }
    )

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(step))
        super.setupProject(project)
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${ArchitecturyMcVersionStep::class.java.name}.version")
    }
}

class ArchitecturyApiVersionStep(parent: NewProjectWizardStep, versions: List<SemanticVersion>) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Architectury API Version:"

    private val useArchitecturyApiProperty = propertyGraph.property(true)
        .bindBooleanStorage("${javaClass.name}.useArchitecturyApi")
    private var useArchitecturyApi by useArchitecturyApiProperty

    override fun setupRow(builder: Row) {
        super.setupRow(builder)

        with(builder) {
            checkBox("Use Architectury API")
                .bindSelected(useArchitecturyApiProperty)
        }

        useArchitecturyApiProperty.afterChange { versionBox.isEnabled = useArchitecturyApi }
        versionBox.isEnabled = useArchitecturyApi
    }

    override fun setupProject(project: Project) {
        if (useArchitecturyApi) {
            data.putUserData(KEY, SemanticVersion.tryParse(version))
        }
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${ArchitecturyApiVersionStep::class.java.name}.version")
    }
}

class ArchitecturyOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(
        ::AuthorsStep,
        ::WebsiteStep,
        ::RepositoryStep,
        ::IssueTrackerStep,
    )
}

class ArchitecturyGradleFilesStep(parent: NewProjectWizardStep) : FixedAssetsNewProjectWizardStep(parent) {
    override fun setupAssets(project: Project) {
        outputDirectory = context.projectFileDirectory

        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modName = data.getUserData(ModNameStep.KEY) ?: return
        val mcVersion = data.getUserData(ArchitecturyMcVersionStep.KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionStep.KEY) ?: return
        val fabricLoaderVersion = data.getUserData(FabricLoaderVersionStep.KEY) ?: return
        val fabricApiVersion = data.getUserData(FabricApiVersionStep.KEY)
        val archApiVersion = data.getUserData(ArchitecturyApiVersionStep.KEY)
        val javaVersion = findStep<JdkProjectSetupFinalizer>().minVersion.ordinal

        addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_NAME" to modName,
            "VERSION" to buildSystemProps.version,
            "MC_VERSION" to mcVersion,
            "FORGE_VERSION" to forgeVersion,
            "FABRIC_LOADER_VERSION" to fabricLoaderVersion,
            "ARCHITECTURY_GROUP" to architecturyGroup,
            "JAVA_VERSION" to javaVersion
        )

        if (fabricApiVersion != null) {
            addTemplateProperties(
                "FABRIC_API_VERSION" to fabricApiVersion,
                "FABRIC_API" to "true",
            )
        }

        if (archApiVersion != null) {
            addTemplateProperties(
                "ARCHITECTURY_API_VERSION" to archApiVersion,
                "ARCHITECTURY_API" to "true",
            )
        }

        addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.ARCHITECTURY_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.ARCHITECTURY_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.ARCHITECTURY_SETTINGS_GRADLE_TEMPLATE,
            "common/build.gradle" to MinecraftTemplates.ARCHITECTURY_COMMON_BUILD_GRADLE_TEMPLATE,
            "forge/build.gradle" to MinecraftTemplates.ARCHITECTURY_FORGE_BUILD_GRADLE_TEMPLATE,
            "forge/gradle.properties" to MinecraftTemplates.ARCHITECTURY_FORGE_GRADLE_PROPERTIES_TEMPLATE,
            "fabric/build.gradle" to MinecraftTemplates.ARCHITECTURY_FABRIC_BUILD_GRADLE_TEMPLATE,
        )

        addGradleWrapperProperties(project)
    }
}

class ArchitecturyProjectCreator(
    private val rootDirectory: Path,
    private val rootModule: Module,
    private val buildSystem: GradleBuildSystem,
    private val config: ArchitecturyProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

//    private val commonModule: Module = project.runWriteTaskInSmartMode {
//        ModuleManager.getInstance(rootModule.project)
//            .newModule(rootDirectory.resolve("common"), ModuleType.get(rootModule).id)
//    }
//    private val forgeModule: Module = project.runWriteTaskInSmartMode {
//        ModuleManager.getInstance(rootModule.project)
//            .newModule(rootDirectory.resolve("forge"), ModuleType.get(rootModule).id)
//    }
//    private val fabricModule: Module = project.runWriteTaskInSmartMode {
//        ModuleManager.getInstance(rootModule.project)
//            .newModule(rootDirectory.resolve("fabric"), ModuleType.get(rootModule).id)
//    }

    override fun getSteps(): Iterable<CreatorStep> {
        val steps = mutableListOf<CreatorStep>()
//        steps += ArchitecturyCommonProjectCreator(
//            rootDirectory.resolve("common"),
//            commonModule,
//            buildSystem,
//            config
//        ).getSteps()
//        steps += ArchitecturyForgeProjectCreator(
//            rootDirectory.resolve("forge"),
//            forgeModule,
//            buildSystem,
//            config
//        ).getSteps()
//        steps += ArchitecturyFabricProjectCreator(
//            rootDirectory.resolve("fabric"),
//            fabricModule,
//            buildSystem,
//            config
//        ).getSteps()
        steps += listOf(
            SimpleGradleSetupStep(
                project,
                rootDirectory,
                buildSystem,
                GradleFiles(
                    ArchitecturyTemplate.applyBuildGradle(project, buildSystem, config),
                    ArchitecturyTemplate.applyGradleProp(project, buildSystem, config),
                    ArchitecturyTemplate.applySettingsGradle(project, buildSystem, config)
                )
            ),
            GradleWrapperStepOld(project, rootDirectory, buildSystem),
            GenRunsStep(project, rootDirectory),
            GradleGitignoreStep(project, rootDirectory),
            CleanUpStep(rootDirectory),
            BasicGradleFinalizerStep(rootModule, rootDirectory, buildSystem)
        )
        return steps
    }

    class GenRunsStep(
        private val project: Project,
        private val rootDirectory: Path
    ) : CreatorStep {

        override fun runStep(indicator: ProgressIndicator) {
            indicator.text = "Setting up project"
            indicator.text2 = "Running Gradle task: 'genIntellijRuns'"
            runGradleTaskAndWait(project, rootDirectory) { settings ->
                settings.taskNames = listOf("genIntellijRuns")
                settings.vmOptions = "-Xmx1G"
            }
            indicator.text2 = null
        }
    }

    class CleanUpStep(
        private val rootDirectory: Path
    ) : CreatorStep {
        override fun runStep(indicator: ProgressIndicator) {
            FileUtils.deleteDirectory(rootDirectory.resolve("src").toFile())
        }
    }
}

class ArchitecturyCommonProjectCreator(
    private val rootDirectory: Path,
    rootModule: Module,
    private val buildSystem: GradleBuildSystem,
    private val config: ArchitecturyProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {
    override fun getSteps(): Iterable<CreatorStep> {
        return listOf(
            CreateDirectoriesStep(buildSystem, rootDirectory),
            ArchitecturyCommonMixinStep(project, buildSystem, config),
            SimpleGradleSetupStep(
                project,
                rootDirectory,
                buildSystem,
                GradleFiles(ArchitecturyTemplate.applyCommonBuildGradle(project, buildSystem, config), null, null)
            ),
            setupMainClassStep()
        )
    }

    private fun setupMainClassStep(): BasicJavaClassStep {
        return BasicJavaClassStep(
            project,
            buildSystem,
            buildSystem.groupId + "." + buildSystem.artifactId + "." + config.pluginName.replace(" ", ""),
            ArchitecturyTemplate.applyCommonMainClass(
                project,
                buildSystem,
                config,
                buildSystem.groupId + "." + buildSystem.artifactId,
                config.pluginName.replace(" ", "")
            )
        )
    }

    class ArchitecturyCommonMixinStep(
        private val project: Project,
        private val buildSystem: BuildSystem,
        private val config: ArchitecturyProjectConfig
    ) : CreatorStep {
        override fun runStep(indicator: ProgressIndicator) {
            if (config.mixins) {
                val text = ArchitecturyTemplate.applyCommonMixinConfigTemplate(project, buildSystem, config)
                val dir = buildSystem.dirsOrError.resourceDirectory
                runWriteTask {
                    CreatorStep.writeTextToFile(project, dir, "${buildSystem.artifactId}-common.mixins.json", text)
                }
            }
        }
    }
}

class ArchitecturyForgeProjectCreator(
    private val rootDirectory: Path,
    rootModule: Module,
    private val buildSystem: GradleBuildSystem,
    private val config: ArchitecturyProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {
    override fun getSteps(): Iterable<CreatorStep> {
        return listOf(
            CreateDirectoriesStep(buildSystem, rootDirectory),
            ArchitecturyForgeMixinStep(project, buildSystem, config),
            ArchitecturyForgeResourcesStep(project, buildSystem, config),
            SimpleGradleSetupStep(
                project,
                rootDirectory,
                buildSystem,
                GradleFiles(
                    ArchitecturyTemplate.applyForgeBuildGradle(project, buildSystem, config),
                    ArchitecturyTemplate.applyForgeGradleProp(project, buildSystem, config),
                    null
                )
            ),
            setupMainClassStep()
        )
    }

    private fun setupMainClassStep(): BasicJavaClassStep {
        return BasicJavaClassStep(
            project,
            buildSystem,
            buildString {
                append(buildSystem.groupId)
                append(".")
                append(buildSystem.artifactId)
                append(".forge.")
                append(config.pluginName.replace(" ", ""))
                append("Forge")
            },
            ArchitecturyTemplate.applyForgeMainClass(
                project,
                buildSystem,
                config,
                buildSystem.groupId + "." + buildSystem.artifactId,
                config.pluginName.replace(" ", "")
            )
        )
    }

    class ArchitecturyForgeMixinStep(
        private val project: Project,
        private val buildSystem: BuildSystem,
        private val config: ArchitecturyProjectConfig
    ) : CreatorStep {
        override fun runStep(indicator: ProgressIndicator) {
            if (config.mixins) {
                val text = ArchitecturyTemplate.applyForgeMixinConfigTemplate(project, buildSystem, config)
                val dir = buildSystem.dirsOrError.resourceDirectory
                runWriteTask {
                    CreatorStep.writeTextToFile(project, dir, "${buildSystem.artifactId}.mixins.json", text)
                }
            }
        }
    }

    class ArchitecturyForgeResourcesStep(
        private val project: Project,
        private val buildSystem: BuildSystem,
        private val config: ArchitecturyProjectConfig
    ) : CreatorStep {
        override fun runStep(indicator: ProgressIndicator) {
            val modsTomlText = ArchitecturyTemplate.applyModsToml(project, buildSystem, config)
            val packDescriptor = ForgePackDescriptor.forMcVersion(config.mcVersion) ?: ForgePackDescriptor.FORMAT_3
            val additionalData = ForgePackAdditionalData.forMcVersion(config.mcVersion)
            val packMcmetaText =
                ArchitecturyTemplate.applyPackMcmeta(project, buildSystem.artifactId, packDescriptor, additionalData)
            val dir = buildSystem.dirsOrError.resourceDirectory
            runWriteTask {
                CreatorStep.writeTextToFile(project, dir, ForgeConstants.PACK_MCMETA, packMcmetaText)
                val meta = dir.resolve("META-INF")
                Files.createDirectories(meta)
                CreatorStep.writeTextToFile(project, meta, ForgeConstants.MODS_TOML, modsTomlText)
            }
        }
    }
}

class ArchitecturyFabricProjectCreator(
    private val rootDirectory: Path,
    rootModule: Module,
    private val buildSystem: GradleBuildSystem,
    private val config: ArchitecturyProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {
    override fun getSteps(): Iterable<CreatorStep> {
        return listOf(
            CreateDirectoriesStep(buildSystem, rootDirectory),
            ArchitecturyFabricMixinStep(project, buildSystem, config),
            ArchitecturyFabricResourcesStep(project, buildSystem, config),
            SimpleGradleSetupStep(
                project,
                rootDirectory,
                buildSystem,
                GradleFiles(
                    ArchitecturyTemplate.applyFabricBuildGradle(project, buildSystem, config),
                    null,
                    null
                )
            ),
            setupMainClassStep()
        )
    }

    private fun setupMainClassStep(): BasicJavaClassStep {
        return BasicJavaClassStep(
            project,
            buildSystem,
            buildString {
                append(buildSystem.groupId)
                append(".")
                append(buildSystem.artifactId)
                append(".fabric.")
                append(config.pluginName.replace(" ", ""))
                append("Fabric")
            },
            ArchitecturyTemplate.applyFabricMainClass(
                project,
                buildSystem,
                config,
                buildSystem.groupId + "." + buildSystem.artifactId,
                config.pluginName.replace(" ", "")
            )
        )
    }

    class ArchitecturyFabricMixinStep(
        private val project: Project,
        private val buildSystem: BuildSystem,
        private val config: ArchitecturyProjectConfig
    ) : CreatorStep {
        override fun runStep(indicator: ProgressIndicator) {
            if (config.mixins) {
                val text = ArchitecturyTemplate.applyFabricMixinConfigTemplate(project, buildSystem, config)
                val dir = buildSystem.dirsOrError.resourceDirectory
                runWriteTask {
                    CreatorStep.writeTextToFile(project, dir, "${buildSystem.artifactId}.mixins.json", text)
                }
            }
        }
    }

    class ArchitecturyFabricResourcesStep(
        private val project: Project,
        private val buildSystem: BuildSystem,
        private val config: ArchitecturyProjectConfig
    ) : CreatorStep {
        override fun runStep(indicator: ProgressIndicator) {
            val text = ArchitecturyTemplate.applyFabricModJsonTemplate(project, buildSystem, config)
            val dir = buildSystem.dirsOrError.resourceDirectory

            indicator.text = "Indexing"

            project.runWriteTaskInSmartMode {
                indicator.text = "Creating 'fabric.mod.json'"

                val file = PsiFileFactory.getInstance(project).createFileFromText(JsonLanguage.INSTANCE, text)
                file.runWriteAction {
                    val jsonFile = file as JsonFile
                    val json = jsonFile.topLevelValue as? JsonObject ?: return@runWriteAction
                    val generator = JsonElementGenerator(project)

                    (json.findProperty("authors")?.value as? JsonArray)?.let { authorsArray ->
                        for (i in config.authors.indices) {
                            if (i != 0) {
                                authorsArray.addBefore(generator.createComma(), authorsArray.lastChild)
                            }
                            authorsArray.addBefore(
                                generator.createStringLiteral(config.authors[i]),
                                authorsArray.lastChild
                            )
                        }
                    }

                    (json.findProperty("contact")?.value as? JsonObject)?.let { contactObject ->
                        val properties = mutableListOf<Pair<String, String>>()
                        val website = config.website
                        if (!website.isNullOrBlank()) {
                            properties += "website" to website
                        }
                        val repo = config.modRepo
                        if (!repo.isNullOrBlank()) {
                            properties += "repo" to repo
                        }
                        val issues = config.modIssue
                        if (!issues.isNullOrBlank()) {
                            properties += "issues" to issues
                        }
                        for (i in properties.indices) {
                            if (i != 0) {
                                contactObject.addBefore(generator.createComma(), contactObject.lastChild)
                            }
                            val key = StringUtil.escapeStringCharacters(properties[i].first)
                            val value = "\"" + StringUtil.escapeStringCharacters(properties[i].second) + "\""
                            contactObject.addBefore(generator.createProperty(key, value), contactObject.lastChild)
                        }
                    }

                    (json.findProperty("entrypoints")?.value as? JsonObject)?.let { entryPointsObject ->
                        val entryPointsByCategory = listOf(
                            EntryPoint(
                                "main",
                                EntryPoint.Type.CLASS,
                                buildString {
                                    append(buildSystem.groupId)
                                    append(".")
                                    append(buildSystem.artifactId)
                                    append(".fabric.")
                                    append(config.pluginName.replace(" ", ""))
                                    append("Fabric")
                                },
                                FabricConstants.MOD_INITIALIZER
                            )
                        )
                            .groupBy { it.category }
                            .asSequence()
                            .sortedBy { it.key }
                            .toList()
                        for (i in entryPointsByCategory.indices) {
                            val entryPointCategory = entryPointsByCategory[i]
                            if (i != 0) {
                                entryPointsObject.addBefore(generator.createComma(), entryPointsObject.lastChild)
                            }
                            val values = generator.createValue<JsonArray>("[]")
                            for (j in entryPointCategory.value.indices) {
                                if (j != 0) {
                                    values.addBefore(generator.createComma(), values.lastChild)
                                }
                                val entryPointReference = entryPointCategory.value[j].computeReference(project)
                                val value = generator.createStringLiteral(entryPointReference)
                                values.addBefore(value, values.lastChild)
                            }
                            val key = StringUtil.escapeStringCharacters(entryPointCategory.key)
                            val prop = generator.createProperty(key, "[]")
                            prop.value?.replace(values)
                            entryPointsObject.addBefore(prop, entryPointsObject.lastChild)
                        }
                    }
                }
                CreatorStep.writeTextToFile(project, dir, FabricConstants.FABRIC_MOD_JSON, file.text)
            }
        }
    }
}
