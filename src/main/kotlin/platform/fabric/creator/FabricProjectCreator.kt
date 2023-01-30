/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.creator

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.AbstractLatentStep
import com.demonwav.mcdev.creator.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AbstractSelectMcVersionThenForkStep
import com.demonwav.mcdev.creator.AbstractSelectVersionStep
import com.demonwav.mcdev.creator.AuthorsStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.LicenseStep
import com.demonwav.mcdev.creator.ModNameStep
import com.demonwav.mcdev.creator.RepositoryStep
import com.demonwav.mcdev.creator.UseMixinsStep
import com.demonwav.mcdev.creator.WaitForSmartModeStep
import com.demonwav.mcdev.creator.WebsiteStep
import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.gradle.addGradleWrapperProperties
import com.demonwav.mcdev.creator.chain
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.platformtype.ModPlatformStep
import com.demonwav.mcdev.platform.fabric.EntryPoint
import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.platform.forge.inspections.sideonly.Side
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_MIXINS_JSON_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_MOD_JSON_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.addMethod
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTaskInSmartMode
import com.demonwav.mcdev.util.toJavaClassName
import com.demonwav.mcdev.util.toPackageName
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.ide.starters.local.GeneratorEmptyDirectory
import com.intellij.ide.util.EditorHelper
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.util.IncorrectOperationException
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.coroutineScope

class FabricPlatformStep(
    parent: ModPlatformStep
) : AbstractLatentStep<Pair<FabricVersions, FabricApiVersions>>(parent) {
    override val description = "download Fabric versions"

    override suspend fun computeData() = coroutineScope {
        val fabricVersions = asyncIO { FabricVersions.downloadData() }
        val fabricApiVersions = asyncIO { FabricApiVersions.downloadData() }
        fabricVersions.await()?.let { a -> fabricApiVersions.await()?.let { b -> a to b } }
    }

    override fun createStep(data: Pair<FabricVersions, FabricApiVersions>): NewProjectWizardStep {
        val (fabricVersions, apiVersions) = data
        return FabricMcVersionStep(this, fabricVersions, apiVersions)
            .chain(
                ::FabricEnvironmentStep,
                ::UseMixinsStep,
                ::ModNameStep,
                ::LicenseStep,
                ::FabricOptionalSettingsStep,
                ::FabricBuildSystemStep,
                ::FabricDumbModeFilesStep,
                ::FabricPostBuildSystemStep,
                ::WaitForSmartModeStep,
                ::FabricSmartModeFilesStep,
            )
    }

    class Factory : ModPlatformStep.Factory {
        override val name = "Fabric"
        override fun createStep(parent: ModPlatformStep) = FabricPlatformStep(parent)
    }
}

class FabricMcVersion(private val ordinal: Int, val version: String) : Comparable<FabricMcVersion> {
    override fun toString() = version
    override fun compareTo(other: FabricMcVersion) = ordinal.compareTo(other.ordinal)
}

class FabricMcVersionStep(
    parent: NewProjectWizardStep,
    private val fabricVersions: FabricVersions,
    private val apiVersions: FabricApiVersions
) : AbstractSelectMcVersionThenForkStep<FabricMcVersion>(
    parent,
    fabricVersions.game.mapIndexed { index, version ->
        FabricMcVersion(fabricVersions.game.size - 1 - index, version.version)
    }
) {
    override val label = "Minecraft Version:"
    private val showSnapshotsProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.showSnapshots")
    private var showSnapshots by showSnapshotsProperty
    init {
        showSnapshotsProperty.afterChange { updateVersionBox() }
    }

    override fun setupSwitcherUi(builder: Row) {
        super.setupSwitcherUi(builder)

        with(builder) {
            checkBox("Show snapshots")
                .bindSelected(showSnapshotsProperty)
        }

        if (!showSnapshots) {
            updateVersionBox()
        }
    }

    private fun updateVersionBox() {
        val selectedItem = versionBox.selectedItem
        versionBox.removeAllItems()
        for (gameVer in fabricVersions.game) {
            if (showSnapshots || gameVer.stable) {
                versionBox.addItem(gameVer.version)
            }
        }
        versionBox.selectedItem = selectedItem
    }

    override fun initStep(version: FabricMcVersion) = FabricLoaderVersionStep(this, fabricVersions.loader)
        .chain(
            { parent ->
                val filteredVersions = fabricVersions.mappings.mapNotNull { mapping ->
                    mapping.version.takeIf { mapping.gameVersion == version.version }
                }
                if (filteredVersions.isEmpty()) {
                    FabricYarnVersionStep(parent, fabricVersions.mappings.map { mapping -> mapping.version }, false)
                } else {
                    FabricYarnVersionStep(parent, filteredVersions, true)
                }
            },
            { parent ->
                val filteredVersions = apiVersions.versions.mapNotNull { api ->
                    api.version.takeIf { version.version in api.gameVersions }
                }
                if (filteredVersions.isEmpty()) {
                    FabricApiVersionStep(parent, apiVersions.versions.map { api -> api.version }, false)
                } else {
                    FabricApiVersionStep(parent, filteredVersions, true)
                }
            },
        )

    override fun setupProject(project: Project) {
        data.putUserData(KEY, step)
        super.setupProject(project)
    }

    companion object {
        val KEY = Key.create<String>("${FabricMcVersionStep::class.java.name}.version")
    }
}

class FabricLoaderVersionStep(parent: NewProjectWizardStep, versions: List<SemanticVersion>) :
    AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Loader Version:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${FabricLoaderVersionStep::class.java.name}.version")
    }
}

class FabricYarnVersionStep(
    parent: NewProjectWizardStep,
    versions: List<FabricVersions.YarnVersion>,
    private val isMatched: Boolean
) :
    AbstractSelectVersionStep<FabricVersions.YarnVersion>(parent, versions) {
    override val label = "Yarn Version:"

    override fun setupRow(builder: Row) {
        super.setupRow(builder)
        if (!isMatched) {
            builder.label("Unable to match Yarn versions to Minecraft version").component.foreground = JBColor.YELLOW
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, version)
    }

    companion object {
        val KEY = Key.create<String>("${FabricYarnVersionStep::class.java.name}.version")
    }
}

class FabricApiVersionStep(
    parent: NewProjectWizardStep,
    versions: List<SemanticVersion>,
    private val isMatched: Boolean
) :
    AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Fabric API Version:"

    private val useFabricApiProperty = propertyGraph.property(true)
        .bindBooleanStorage("${javaClass.name}.useFabricApi")
    private var useFabricApi by useFabricApiProperty

    override fun setupRow(builder: Row) {
        super.setupRow(builder)

        with(builder) {
            checkBox("Use Fabric API")
                .bindSelected(useFabricApiProperty)

            if (!isMatched) {
                label("Unable to match API versions to Minecraft version").component.foreground = JBColor.YELLOW
            }
        }

        useFabricApiProperty.afterChange { versionBox.isEnabled = useFabricApi }
        versionBox.isEnabled = useFabricApi
    }

    override fun setupProject(project: Project) {
        if (useFabricApi) {
            data.putUserData(KEY, SemanticVersion.tryParse(version))
        }
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${FabricApiVersionStep::class.java.name}.version")
    }
}

class FabricEnvironmentStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val environmentProperty = propertyGraph.property(Side.NONE)
    init {
        environmentProperty.transform(Side::name, Side::valueOf).bindStorage("${javaClass.name}.side")
    }
    private var environment by environmentProperty

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("Environment:") {
                comboBox(listOf("Both", "Client", "Server"))
                    .bindItem(
                        environmentProperty.transform({
                            when (it) {
                                Side.CLIENT -> "Client"
                                Side.SERVER -> "Server"
                                else -> "Both"
                            }
                        }, {
                            when (it) {
                                "Client" -> Side.CLIENT
                                "Server" -> Side.SERVER
                                else -> Side.NONE
                            }
                        })
                    )
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, environment)
    }

    companion object {
        val KEY = Key.create<Side>("${FabricEnvironmentStep::class.java.name}.environment")
    }
}

class FabricOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(::AuthorsStep, ::WebsiteStep, ::RepositoryStep)
}

class FabricGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val mcVersion = data.getUserData(FabricMcVersionStep.KEY) ?: return
        val yarnVersion = data.getUserData(FabricYarnVersionStep.KEY) ?: return
        val loaderVersion = data.getUserData(FabricLoaderVersionStep.KEY) ?: return
        val loomVersion = "1.0-SNAPSHOT" // TODO
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val apiVersion = data.getUserData(FabricApiVersionStep.KEY)

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "VERSION" to buildSystemProps.version,
            "MC_VERSION" to mcVersion,
            "YARN_MAPPINGS" to yarnVersion,
            "LOADER_VERSION" to loaderVersion,
            "LOOM_VERSION" to loomVersion,
            "JAVA_VERSION" to javaVersion,
        )

        if (apiVersion != null) {
            assets.addTemplateProperties("API_VERSION" to apiVersion)
        }

        assets.addTemplates(
            project,
            "build.gradle" to FABRIC_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to FABRIC_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to FABRIC_SETTINGS_GRADLE_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)
    }
}

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
        val environment = data.getUserData(FabricEnvironmentStep.KEY) ?: Side.NONE
        val envName = when (environment) {
            Side.CLIENT -> "client"
            Side.SERVER -> "server"
            else -> "*"
        }
        val loaderVersion = data.getUserData(FabricLoaderVersionStep.KEY) ?: return
        val mcVersion = data.getUserData(FabricMcVersionStep.KEY) ?: return
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val apiVersion = data.getUserData(FabricApiVersionStep.KEY)
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
                GlobalSearchScope.projectScope(project)
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
                            e.localizedMessage
                        )
                        Messages.showErrorDialog(
                            project,
                            message,
                            MCDevBundle.message("intention.error.cannot.create.class.title")
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
    parent: NewProjectWizardStep
) : AbstractRunBuildSystemStep(parent, FabricBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}

class FabricGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> FabricGradleFilesStep(parent).chain(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent)
            else -> EmptyStep(parent)
        }
    }
}
