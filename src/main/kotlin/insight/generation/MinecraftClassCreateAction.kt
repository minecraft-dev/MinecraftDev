/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight.generation

import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.daemon.JavaErrorBundle
import com.intellij.codeInsight.daemon.impl.analysis.HighlightClassUtil
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.actions.CreateTemplateInPackageAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.util.PsiUtil
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes

class MinecraftClassCreateAction :
    CreateTemplateInPackageAction<PsiClass>(
        CAPTION,
        "Class generation for modders",
        GeneralAssets.MC_TEMPLATE,
        JavaModuleSourceRootTypes.SOURCES
    ),
    DumbAware {

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String = CAPTION

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(CAPTION)
        builder.setValidator(ClassInputValidator(project, directory))

        val module = directory.findModule() ?: return
        val isForge = MinecraftFacet.getInstance(module, ForgeModuleType) != null
        val isFabric = MinecraftFacet.getInstance(module, FabricModuleType) != null
        val mcVersion = MinecraftFacet.getInstance(module, McpModuleType)?.getSettings()
            ?.minecraftVersion?.let(SemanticVersion::parse)

        if (isForge && mcVersion != null) {
            val icon = PlatformAssets.FORGE_ICON

            if (mcVersion < MinecraftVersions.MC1_17) {
                builder.addKind("Block", icon, MinecraftTemplates.FORGE_BLOCK_TEMPLATE)
                builder.addKind("Item", icon, MinecraftTemplates.FORGE_ITEM_TEMPLATE)
                builder.addKind("Packet", icon, MinecraftTemplates.FORGE_PACKET_TEMPLATE)
                builder.addKind("Enchantment", icon, MinecraftTemplates.FORGE_ENCHANTMENT_TEMPLATE)
            } else if (mcVersion < MinecraftVersions.MC1_18) {
                builder.addKind("Block", icon, MinecraftTemplates.FORGE_1_17_BLOCK_TEMPLATE)
                builder.addKind("Item", icon, MinecraftTemplates.FORGE_1_17_ITEM_TEMPLATE)
                builder.addKind("Packet", icon, MinecraftTemplates.FORGE_1_17_PACKET_TEMPLATE)
                builder.addKind("Enchantment", icon, MinecraftTemplates.FORGE_1_17_ENCHANTMENT_TEMPLATE)
            } else {
                builder.addKind("Block", icon, MinecraftTemplates.FORGE_1_17_BLOCK_TEMPLATE)
                builder.addKind("Item", icon, MinecraftTemplates.FORGE_1_17_ITEM_TEMPLATE)
                builder.addKind("Packet", icon, MinecraftTemplates.FORGE_1_18_PACKET_TEMPLATE)
                builder.addKind("Enchantment", icon, MinecraftTemplates.FORGE_1_17_ENCHANTMENT_TEMPLATE)
            }
        }
        if (isFabric) {
            val icon = PlatformAssets.FABRIC_ICON

            builder.addKind("Block", icon, MinecraftTemplates.FABRIC_BLOCK_TEMPLATE)
            builder.addKind("Item", icon, MinecraftTemplates.FABRIC_ITEM_TEMPLATE)
            builder.addKind("Enchantment", icon, MinecraftTemplates.FABRIC_ENCHANTMENT_TEMPLATE)
        }
    }

    override fun isAvailable(dataContext: DataContext): Boolean {
        val psi = dataContext.getData(CommonDataKeys.PSI_ELEMENT)
        val module = psi?.findModule() ?: return false
        val isFabricMod = MinecraftFacet.getInstance(module, FabricModuleType) != null
        val isForgeMod = MinecraftFacet.getInstance(module, ForgeModuleType) != null
        val hasMcVersion = MinecraftFacet.getInstance(module, McpModuleType)?.getSettings()?.minecraftVersion != null

        return (isFabricMod || isForgeMod && hasMcVersion) && super.isAvailable(dataContext)
    }

    override fun checkPackageExists(directory: PsiDirectory): Boolean {
        val pkg = JavaDirectoryService.getInstance().getPackage(directory) ?: return false

        val name = pkg.qualifiedName
        return StringUtil.isEmpty(name) || PsiNameHelper.getInstance(directory.project).isQualifiedName(name)
    }

    override fun getNavigationElement(createdElement: PsiClass): PsiElement? {
        return createdElement.lBrace
    }

    override fun doCreate(dir: PsiDirectory, className: String, templateName: String): PsiClass? {
        return JavaDirectoryService.getInstance().createClass(dir, className, templateName, false)
    }

    private class ClassInputValidator(
        private val project: Project,
        private val directory: PsiDirectory
    ) : InputValidatorEx {
        override fun getErrorText(inputString: String): String? {
            if (inputString.isNotEmpty() && !PsiNameHelper.getInstance(project).isQualifiedName(inputString)) {
                return JavaErrorBundle.message("create.class.action.this.not.valid.java.qualified.name")
            }

            val shortName = StringUtil.getShortName(inputString)
            val languageLevel = PsiUtil.getLanguageLevel(directory)
            return if (HighlightClassUtil.isRestrictedIdentifier(shortName, languageLevel)) {
                JavaErrorBundle.message("restricted.identifier", shortName)
            } else {
                null
            }
        }

        override fun checkInput(inputString: String): Boolean =
            inputString.isNotBlank() && getErrorText(inputString) == null

        override fun canClose(inputString: String): Boolean =
            checkInput(inputString)
    }

    private companion object {
        private const val CAPTION = "Minecraft Class"
    }
}
