/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.creator

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.RepositoryStep
import com.demonwav.mcdev.creator.step.UseMixinsStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.fabric.EntryPoint
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.forge.inspections.sideonly.Side
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_MIXINS_JSON_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_MOD_JSON_TEMPLATE
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.addMethod
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTaskInSmartMode
import com.demonwav.mcdev.util.toJavaClassName
import com.demonwav.mcdev.util.toPackageName
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.ide.starters.local.GeneratorEmptyDirectory
import com.intellij.ide.util.EditorHelper
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.IncorrectOperationException
import java.nio.file.Path
import java.util.concurrent.CountDownLatch

class FabricDumbModeFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Adding Fabric project files (phase 1)"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal

        if (useMixins) {
            val packageName =
                "${buildSystemProps.groupId.toPackageName()}.${buildSystemProps.artifactId.toPackageName()}.mixin"
            assets.addTemplateProperties(
                "PACKAGE_NAME" to packageName,
                "JAVA_VERSION" to javaVersion,
            )
            val mixinsJsonFile = "src/main/resources/${buildSystemProps.artifactId}.mixins.json"
            assets.addTemplates(project, mixinsJsonFile to FABRIC_MIXINS_JSON_TEMPLATE)
        }

        assets.addLicense(project)

        assets.addAssets(
            GeneratorEmptyDirectory("src/main/java"),
            GeneratorEmptyDirectory("src/main/resources"),
        )
    }
}

class FabricSmartModeFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Adding Fabric project files (phase 2)"

    private lateinit var entryPoints: List<EntryPoint>

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val envName = when (data.getUserData(FabricEnvironmentStep.KEY) ?: Side.NONE) {
            Side.CLIENT -> "client"
            Side.SERVER -> "server"
            else -> "*"
        }
        val loaderVersion = data.getUserData(FabricVersionChainStep.LOADER_VERSION_KEY) ?: return
        val mcVersion = data.getUserData(FabricVersionChainStep.MC_VERSION_KEY) ?: return
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val apiVersion = data.getUserData(FabricVersionChainStep.API_VERSION_KEY)
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false

        val packageName = "${buildSystemProps.groupId.toPackageName()}.${buildSystemProps.artifactId.toPackageName()}"
        val mainClassName = "$packageName.${modName.toJavaClassName()}"
        val clientClassName = "$packageName.client.${modName.toJavaClassName()}Client"
        entryPoints = listOf(
            EntryPoint("main", EntryPoint.Type.CLASS, mainClassName, FabricConstants.MOD_INITIALIZER),
            EntryPoint("client", EntryPoint.Type.CLASS, clientClassName, FabricConstants.CLIENT_MOD_INITIALIZER),
        ) // TODO: un-hardcode?

        assets.addTemplateProperties(
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_NAME" to StringUtil.escapeStringCharacters(modName),
            "MOD_DESCRIPTION" to StringUtil.escapeStringCharacters(description),
            "MOD_ENVIRONMENT" to envName,
            "LOADER_VERSION" to loaderVersion,
            "MC_VERSION" to mcVersion,
            "JAVA_VERSION" to javaVersion,
            "LICENSE" to license.id,
        )

        if (apiVersion != null) {
            assets.addTemplateProperties("API_VERSION" to apiVersion)
        }

        if (useMixins) {
            assets.addTemplateProperties("MIXINS" to "true")
        }

        assets.addTemplates(project, "src/main/resources/fabric.mod.json" to FABRIC_MOD_JSON_TEMPLATE)
    }

    private fun fixupFabricModJson(project: Project) {
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val website = data.getUserData(WebsiteStep.KEY)
        val repo = data.getUserData(RepositoryStep.KEY)

        val fabricModJsonFile =
            VfsUtil.findFile(Path.of(context.projectFileDirectory, "src", "main", "resources", "fabric.mod.json"), true)
                ?: return
        val jsonFile = PsiManager.getInstance(project).findFile(fabricModJsonFile) as? JsonFile ?: return
        val json = jsonFile.topLevelValue as? JsonObject ?: return
        val generator = JsonElementGenerator(project)

        NonProjectFileWritingAccessProvider.allowWriting(listOf(fabricModJsonFile))
        jsonFile.runWriteAction {
            (json.findProperty("authors")?.value as? JsonArray)?.let { authorsArray ->
                for (i in authors.indices) {
                    if (i != 0) {
                        authorsArray.addBefore(generator.createComma(), authorsArray.lastChild)
                    }
                    authorsArray.addBefore(generator.createStringLiteral(authors[i]), authorsArray.lastChild)
                }
            }

            (json.findProperty("contact")?.value as? JsonObject)?.let { contactObject ->
                val properties = mutableListOf<Pair<String, String>>()
                if (!website.isNullOrBlank()) {
                    properties += "website" to website
                }
                if (!repo.isNullOrBlank()) {
                    properties += "repo" to repo
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
                val entryPointsByCategory = entryPoints
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

            ReformatCodeProcessor(project, jsonFile, null, false).run()
        }
    }

    private fun createEntryPoints(project: Project) {
        val root = VfsUtil.findFile(Path.of(context.projectFileDirectory), true) ?: return
        val psiManager = PsiManager.getInstance(project)

        val generatedClasses = mutableSetOf<PsiClass>()

        for (entryPoint in entryPoints) {
            // find the class, and create it if it doesn't exist
            val clazz = JavaPsiFacade.getInstance(project).findClass(
                entryPoint.className,
                GlobalSearchScope.projectScope(project),
            ) ?: run {
                val packageName = entryPoint.className.substringBeforeLast('.', missingDelimiterValue = "")
                val className = entryPoint.className.substringAfterLast('.')

                val dir = VfsUtil.createDirectoryIfMissing(root, "src/main/java/${packageName.replace('.', '/')}")
                val psiDir = psiManager.findDirectory(dir) ?: return@run null
                try {
                    JavaDirectoryService.getInstance().createClass(psiDir, className)
                } catch (e: IncorrectOperationException) {
                    invokeLater {
                        val message = MCDevBundle.message(
                            "intention.error.cannot.create.class.message",
                            className,
                            e.localizedMessage,
                        )
                        Messages.showErrorDialog(
                            project,
                            message,
                            MCDevBundle.message("intention.error.cannot.create.class.title"),
                        )
                    }
                    return
                }
            } ?: continue

            clazz.containingFile.runWriteAction {
                clazz.addImplements(entryPoint.interfaceName)

                val methodsToImplement = OverrideImplementUtil.getMethodsToOverrideImplement(clazz, true)
                val methods = OverrideImplementUtil.overrideOrImplementMethodCandidates(clazz, methodsToImplement, true)
                for (method in methods) {
                    clazz.addMethod(method)
                }
            }

            generatedClasses += clazz
        }

        for (clazz in generatedClasses) {
            ReformatCodeProcessor(project, clazz.containingFile, null, false).run()
            EditorHelper.openInEditor(clazz)
        }
    }

    override fun perform(project: Project) {
        super.perform(project)
        val latch = CountDownLatch(1)
        assets.runWhenCreated(project) {
            project.runWriteTaskInSmartMode {
                try {
                    fixupFabricModJson(project)
                    createEntryPoints(project)
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
    }
}

class FabricBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Fabric"
}

class FabricPostBuildSystemStep(
    parent: NewProjectWizardStep,
) : AbstractRunBuildSystemStep(parent, FabricBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}
