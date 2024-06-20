/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isInSpongePluginClass
import com.demonwav.mcdev.platform.sponge.util.isInjectOptional
import com.demonwav.mcdev.platform.sponge.util.spongePluginClassId
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.util.PackageUtil
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.createSmartPointer
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspection.formatString
import com.siyeh.ig.ui.UiUtils
import javax.swing.JComponent
import org.jdom.Element

class SpongeInjectionInspection : AbstractBaseJavaLocalInspectionTool() {

    private val injectableTypesList = defaultInjectableTypes.toMutableList()

    @JvmField
    var injectableTypes: String = serializedDefaultInjectableTypes

    override fun getStaticDescription() = "Invalid @Inject usage in Sponge plugin class."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return if (SpongeModuleType.isInModule(holder.file)) {
            Visitor(holder)
        } else {
            PsiElementVisitor.EMPTY_VISITOR
        }
    }

    override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (SpongeModuleType.isInModule(file)) {
            super.processFile(file, manager)
        } else {
            emptyList()
        }
    }

    private inner class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitField(field: PsiField) {
            if (!field.hasAnnotation(SpongeConstants.INJECT_ANNOTATION) || !field.isInSpongePluginClass()) {
                return
            }

            checkInjection(field, field)
        }

        override fun visitMethod(method: PsiMethod) {
            if (!method.hasAnnotation(SpongeConstants.INJECT_ANNOTATION) || !method.isInSpongePluginClass()) {
                return
            }

            if (method.isConstructor) {
                val annotation = method.getAnnotation(SpongeConstants.INJECT_ANNOTATION)
                if (annotation != null && isInjectOptional(annotation)) {
                    holder.registerProblem(
                        annotation.parameterList,
                        "Constructor injection cannot be optional.",
                        ProblemHighlightType.GENERIC_ERROR,
                        RemoveAnnotationParameters(annotation, "Remove 'optional' parameter"),
                    )
                }
            }

            method.parameterList.parameters.forEach { variable -> this.checkInjection(variable, variable) }
        }

        private fun checkInjection(variable: PsiVariable, annotationsOwner: PsiModifierListOwner) {
            val typeElement = variable.typeElement ?: return
            if (variable.type is PsiPrimitiveType) {
                holder.registerProblem(
                    typeElement,
                    "Primitive types cannot be injected by Sponge.",
                    ProblemHighlightType.GENERIC_ERROR,
                )
                return
            }

            val classType = variable.type as? PsiClassReferenceType ?: return
            val name = classType.resolve()?.qualifiedName ?: return

            // Check if injection is possible with the current type
            if (name !in injectableTypesList) {
                holder.registerProblem(
                    typeElement,
                    "${classType.presentableText} cannot be injected by Sponge.",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    AddToKnownInjectableTypes(name),
                )
                return
            }

            checkAssetId(name, variable, annotationsOwner)

            // @ConfigDir and @DefaultConfig usages
            checkConfig(variable, name, classType, annotationsOwner)
        }

        private fun checkAssetId(
            name: String,
            variable: PsiVariable,
            annotationsOwner: PsiModifierListOwner,
        ) {
            if (name != "org.spongepowered.api.asset.Asset") {
                return
            }

            val assetId = variable.getAnnotation(SpongeConstants.ASSET_ID_ANNOTATION)
            if (assetId == null) {
                holder.registerProblem(
                    variable.nameIdentifier ?: variable,
                    "Injected Assets must be annotated with @AssetId.",
                    ProblemHighlightType.GENERIC_ERROR,
                    AddAnnotationFix(SpongeConstants.ASSET_ID_ANNOTATION, annotationsOwner),
                )
            } else {
                variable.findModule()?.let { module ->
                    val assetPathAttributeValue = assetId.findAttributeValue(null)
                    val assetPath = assetPathAttributeValue?.constantStringValue?.replace('\\', '/') ?: return@let

                    val pluginId = variable.spongePluginClassId() ?: return@let
                    val relativeAssetPath = "assets/$pluginId/$assetPath"

                    val roots = ModuleRootManager.getInstance(module).getSourceRoots(false)
                    val assetFile = roots.mapFirstNotNull { root ->
                        root.findFileByRelativePath(relativeAssetPath)
                    }
                    if (assetFile?.isDirectory == true || relativeAssetPath.endsWith('/')) {
                        holder.registerProblem(
                            assetPathAttributeValue,
                            "AssetId must reference a file.",
                            ProblemHighlightType.GENERIC_ERROR,
                        )
                        return@let
                    }

                    if (assetFile == null) {
                        val fix = roots.firstOrNull()?.let { contentRoot ->
                            variable.manager.findDirectory(contentRoot)?.let { directory ->
                                CreateAssetFileFix(assetPath, module, pluginId, directory)
                            }
                        }

                        if (fix == null) {
                            holder.registerProblem(
                                assetPathAttributeValue,
                                "Asset '$assetPath' does not exist.",
                                ProblemHighlightType.GENERIC_ERROR,
                            )
                        } else {
                            holder.registerProblem(
                                assetPathAttributeValue,
                                "Asset '$assetPath' does not exist.",
                                ProblemHighlightType.GENERIC_ERROR,
                                fix,
                            )
                        }
                    }
                }
            }
        }

        private fun checkConfig(
            variable: PsiVariable,
            name: String,
            classType: PsiClassReferenceType,
            annotationsOwner: PsiModifierListOwner,
        ) {
            val configDir = variable.getAnnotation(SpongeConstants.CONFIG_DIR_ANNOTATION)
            val defaultConfig = variable.getAnnotation(SpongeConstants.DEFAULT_CONFIG_ANNOTATION)

            when (name) {
                "java.io.File", "java.nio.file.Path" -> {
                    if (configDir != null && defaultConfig != null) {
                        val quickFixFactory = QuickFixFactory.getInstance()
                        holder.registerProblem(
                            variable.nameIdentifier ?: variable,
                            "@ConfigDir and @DefaultConfig cannot be used on the same field.",
                            ProblemHighlightType.GENERIC_ERROR,
                            quickFixFactory.createDeleteFix(configDir, "Remove @ConfigDir"),
                            quickFixFactory.createDeleteFix(defaultConfig, "Remove @DefaultConfig"),
                        )
                    } else if (configDir == null && defaultConfig == null) {
                        holder.registerProblem(
                            variable.nameIdentifier ?: variable,
                            "An injected ${classType.name} must be annotated with either @ConfigDir or @DefaultConfig.",
                            ProblemHighlightType.GENERIC_ERROR,
                            AddAnnotationFix(SpongeConstants.CONFIG_DIR_ANNOTATION, annotationsOwner),
                            AddAnnotationFix(SpongeConstants.DEFAULT_CONFIG_ANNOTATION, annotationsOwner),
                        )
                    }
                }
                "ninja.leaping.configurate.loader.ConfigurationLoader",
                "org.spongepowered.configurate.reference.ConfigurationReference",
                "org.spongepowered.configurate.loader.ConfigurationLoader" -> {
                    if (defaultConfig == null) {
                        holder.registerProblem(
                            variable.nameIdentifier ?: variable,
                            "Injected ${classType.name} must be annotated with @DefaultConfig.",
                            ProblemHighlightType.GENERIC_ERROR,
                            AddAnnotationFix(SpongeConstants.DEFAULT_CONFIG_ANNOTATION, annotationsOwner),
                        )
                    }

                    if (configDir != null) {
                        holder.registerProblem(
                            configDir,
                            "Injected ${classType.name} cannot be annotated with @ConfigDir.",
                            ProblemHighlightType.GENERIC_ERROR,
                            QuickFixFactory.getInstance().createDeleteFix(configDir, "Remove @ConfigDir"),
                        )
                    }

                    if (classType.isRaw) {
                        val ref = classType.reference
                        holder.registerProblem(
                            ref,
                            "Injected ${classType.name} must have a generic parameter.",
                            ProblemHighlightType.GENERIC_ERROR,
                            MissingConfLoaderTypeParamFix(ref),
                        )
                    } else {
                        classType.parameters.firstOrNull()?.let { param ->
                            val paramType = param as? PsiClassReferenceType ?: return@let
                            val paramTypeFQName = paramType.fullQualifiedName ?: return@let
                            if (
                                paramTypeFQName != "ninja.leaping.configurate.commented.CommentedConfigurationNode" &&
                                paramTypeFQName != "org.spongepowered.configurate.CommentedConfigurationNode"
                            ) {
                                val ref = param.reference
                                holder.registerProblem(
                                    ref,
                                    "Injected ConfigurationLoader generic parameter must be " +
                                        "CommentedConfigurationNode.",
                                    ProblemHighlightType.GENERIC_ERROR,
                                    WrongConfLoaderTypeParamFix(classType.className, ref),
                                )
                            }
                        }
                    }
                }
                else -> {
                    val quickFixFactory = QuickFixFactory.getInstance()
                    if (configDir != null) {
                        holder.registerProblem(
                            configDir,
                            "${classType.name} cannot be annotated with @ConfigDir.",
                            ProblemHighlightType.GENERIC_ERROR,
                            quickFixFactory.createDeleteFix(configDir, "Remove @ConfigDir"),
                        )
                    }

                    if (defaultConfig != null) {
                        holder.registerProblem(
                            defaultConfig,
                            "${classType.name} cannot be annotated with @DefaultConfig.",
                            ProblemHighlightType.GENERIC_ERROR,
                            quickFixFactory.createDeleteFix(defaultConfig, "Remove @DefaultConfig"),
                        )
                    }
                }
            }
        }
    }

    companion object {

        val defaultInjectableTypes = listOf(
            // https://github.com/SpongePowered/SpongeCommon/blob/f92cef4/src/main/java/org/spongepowered/common/inject/SpongeImplementationModule.java
            "org.spongepowered.api.Game",
            "org.spongepowered.api.MinecraftVersion",
            "org.spongepowered.api.service.ServiceManager",
            "org.spongepowered.api.asset.AssetManager",
            "org.spongepowered.api.GameRegistry",
            "org.spongepowered.api.world.TeleportHelper",
            "org.spongepowered.api.scheduler.Scheduler",
            "org.spongepowered.api.command.CommandManager",
            "org.spongepowered.api.data.DataManager",
            "org.spongepowered.api.config.ConfigManager",
            "org.spongepowered.api.data.property.PropertyRegistry",
            "org.spongepowered.api.event.CauseStackManager",
            "org.spongepowered.api.util.metric.MetricsConfigManager",
            "org.spongepowered.api.Platform",
            "org.spongepowered.api.plugin.PluginManager",
            "org.spongepowered.api.event.EventManager",
            "org.spongepowered.api.network.ChannelRegistrar",
            "org.slf4j.Logger",
            "org.apache.logging.log4j.Logger",
            // API 8 (plugin-spi)
            "org.spongepowered.plugin.PluginContainer",
            // https://github.com/SpongePowered/SpongeCommon/blob/855ffc1/src/main/java/org/spongepowered/common/inject/SpongeModule.java
            "java.io.File",
            "java.nio.file.Path",
            "ninja.leaping.configurate.loader.ConfigurationLoader",
            // Configurate 4
            "org.spongepowered.configurate.reference.ConfigurationReference",
            "org.spongepowered.configurate.loader.ConfigurationLoader",

            "ninja.leaping.configurate.objectmapping.GuiceObjectMapperFactory",
            "com.google.inject.Injector",
            "org.spongepowered.api.plugin.PluginContainer",
            "org.spongepowered.api.asset.Asset",

            "org.bstats.sponge.MetricsLite2.Factory",
            "org.bstats.sponge.Metrics2.Factory",
        )

        val serializedDefaultInjectableTypes: String = formatString(defaultInjectableTypes)
    }

    class RemoveAnnotationParameters(annotation: PsiAnnotation, val txt: String) :
        LocalQuickFixOnPsiElement(annotation) {

        override fun getFamilyName(): String = name

        override fun getText(): String = txt

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val newAnnotation = JavaPsiFacade.getElementFactory(project)
                .createAnnotationFromText("@" + (startElement as PsiAnnotation).qualifiedName, startElement.parent)
            startElement.replace(newAnnotation)
        }
    }

    class WrongConfLoaderTypeParamFix(private val clazzName: String, param: PsiJavaCodeReferenceElement) :
        LocalQuickFixOnPsiElement(param) {

        override fun getFamilyName(): String = name

        override fun getText(): String = "Set to CommentedConfigurationNode"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val newRef = JavaPsiFacade.getElementFactory(project).createReferenceFromText(
                when (clazzName) {
                    "ninja.leaping.configurate.loader.ConfigurationLoader" ->
                        "ninja.leaping.configurate.commented.CommentedConfigurationNode"
                    else -> { "org.spongepowered.configurate.CommentedConfigurationNode" }
                },
                startElement,
            )
            startElement.replace(newRef)
        }
    }

    class MissingConfLoaderTypeParamFix(ref: PsiJavaCodeReferenceElement) : LocalQuickFixOnPsiElement(ref) {

        override fun getFamilyName(): String = name

        override fun getText(): String = "Insert generic parameter"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val newRef: PsiElement = if (
                JavaPsiFacade.getInstance(project)
                    .findPackage("ninja.leaping.configurate") != null
            ) {
                JavaPsiFacade.getElementFactory(project).createReferenceFromText(
                    "ninja.leaping.configurate.loader.ConfigurationLoader" +
                        "<ninja.leaping.configurate.commented.CommentedConfigurationNode>",
                    startElement
                )
            } else {
                JavaPsiFacade.getElementFactory(project).createReferenceFromText(
                    "org.spongepowered.configurate.loader.ConfigurationLoader" +
                        "<org.spongepowered.configurate.CommentedConfigurationNode>",
                    startElement
                )
            }

            startElement.replace(newRef)
        }
    }

    class CreateAssetFileFix(
        private val assetPath: String,
        private val module: Module,
        private val pluginId: String,
        directory: PsiDirectory,
    ) : LocalQuickFix {

        private val dirPointer = directory.createSmartPointer()

        override fun getFamilyName(): String = "Create asset file"

        override fun startInWriteAction(): Boolean = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val index = assetPath.lastIndexOf('/')
            val assetDir = if (index > 0) {
                "." + assetPath.substring(0, index).replace('/', '.')
            } else {
                ""
            }
            val fileName = assetPath.substring(index + 1)
            val createdDir = PackageUtil.findOrCreateDirectoryForPackage(
                module,
                "assets.$pluginId$assetDir",
                dirPointer.element,
                false,
            )
            val createdFile = runWriteAction {
                createdDir?.createFile(fileName)
            } ?: return
            if (createdFile.canNavigate()) {
                createdFile.navigate(true)
            }
        }
    }

    inner class AddToKnownInjectableTypes(private val typeFqn: String) : LocalQuickFix {
        override fun getFamilyName(): String = "Add to known injectable types"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            injectableTypesList.add(typeFqn)
        }
    }

    override fun createOptionsPanel(): JComponent? {
        val chooserList = UiUtils.createTreeClassChooserList(
            injectableTypesList,
            "Injectable types",
            "Choose injectable type",
        )
        UiUtils.setComponentSize(chooserList, 7, 25)
        return chooserList
    }

    override fun readSettings(node: Element) {
        super.readSettings(node)
        BaseInspection.parseString(injectableTypes, injectableTypesList)
    }

    override fun writeSettings(node: Element) {
        injectableTypes = if (injectableTypesList.isEmpty()) {
            serializedDefaultInjectableTypes
        } else {
            formatString(injectableTypesList)
        }

        super.writeSettings(node)
    }
}
