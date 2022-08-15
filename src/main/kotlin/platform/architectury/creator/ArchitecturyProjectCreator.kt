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

import com.demonwav.mcdev.creator.BaseProjectCreator
import com.demonwav.mcdev.creator.BasicJavaClassStep
import com.demonwav.mcdev.creator.CreateDirectoriesStep
import com.demonwav.mcdev.creator.CreatorStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.BasicGradleFinalizerStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleFiles
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleGitignoreStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.gradle.SimpleGradleSetupStep
import com.demonwav.mcdev.platform.fabric.EntryPoint
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.forge.util.ForgePackAdditionalData
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.util.runGradleTaskAndWait
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.demonwav.mcdev.util.runWriteTaskInSmartMode
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFileFactory
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils

class ArchitecturyProjectCreator(
    private val rootDirectory: Path,
    private val rootModule: Module,
    private val buildSystem: GradleBuildSystem,
    private val config: ArchitecturyProjectConfig
) : BaseProjectCreator(rootModule, buildSystem) {

    private val commonModule: Module = project.runWriteTaskInSmartMode {
        ModuleManager.getInstance(rootModule.project)
            .newModule(rootDirectory.resolve("common"), ModuleType.get(rootModule).id)
    }
    private val forgeModule: Module = project.runWriteTaskInSmartMode {
        ModuleManager.getInstance(rootModule.project)
            .newModule(rootDirectory.resolve("forge"), ModuleType.get(rootModule).id)
    }
    private val fabricModule: Module = project.runWriteTaskInSmartMode {
        ModuleManager.getInstance(rootModule.project)
            .newModule(rootDirectory.resolve("fabric"), ModuleType.get(rootModule).id)
    }

    override fun getSteps(): Iterable<CreatorStep> {
        val steps = mutableListOf<CreatorStep>()
        steps += ArchitecturyCommonProjectCreator(
            rootDirectory.resolve("common"),
            commonModule,
            buildSystem,
            config
        ).getSteps()
        steps += ArchitecturyForgeProjectCreator(
            rootDirectory.resolve("forge"),
            forgeModule,
            buildSystem,
            config
        ).getSteps()
        steps += ArchitecturyFabricProjectCreator(
            rootDirectory.resolve("fabric"),
            fabricModule,
            buildSystem,
            config
        ).getSteps()
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
            GradleWrapperStep(project, rootDirectory, buildSystem),
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
