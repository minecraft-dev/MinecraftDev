/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight.generation

import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.findModule
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.actions.CreateTemplateInPackageAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameHelper
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

        val module = directory.findModule()
        val isForge = module?.let { MinecraftFacet.getInstance(it, ForgeModuleType) } != null
        val isFabric = module?.let { MinecraftFacet.getInstance(it, FabricModuleType) } != null

        if (isForge) {
            val icon = PlatformAssets.FORGE_ICON

            builder.addKind("Block", icon, MinecraftTemplates.FORGE_BLOCK_TEMPLATE)
            builder.addKind("Item", icon, MinecraftTemplates.FORGE_ITEM_TEMPLATE)
            builder.addKind("Packet", icon, MinecraftTemplates.FORGE_PACKET_TEMPLATE)
            builder.addKind("Enchantment", icon, MinecraftTemplates.FORGE_ENCHANTMENT_TEMPLATE)
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
        val isModModule = psi?.findModule()?.let {
            MinecraftFacet.getInstance(it, FabricModuleType, ForgeModuleType)
        } != null

        return isModModule && super.isAvailable(dataContext)
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

    private companion object {
        private const val CAPTION = "Minecraft Class"
    }
}
