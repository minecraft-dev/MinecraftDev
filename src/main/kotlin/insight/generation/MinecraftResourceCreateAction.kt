package com.demonwav.mcdev.insight.generation

import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.MinecraftClassCreateAction.ClassInputValidator
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.neoforge.NeoForgeModuleType
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.findModule
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import java.util.*
import org.jetbrains.annotations.NonNls
import org.jetbrains.jps.model.java.JavaResourceRootType


class MinecraftResourceCreateAction : CreateFileFromTemplateAction(
    Const.CAPTION,
    MCDevBundle("generate.class.description"),
    GeneralAssets.MC_TEMPLATE
) {
    override fun isAvailable(context: DataContext): Boolean {
        val psi = context.getData(CommonDataKeys.PSI_ELEMENT)
        val module = psi?.findModule() ?: return false
        val dir: PsiDirectory?
        if (psi is PsiFile) {
            dir = psi.containingDirectory
        } else  if(psi is PsiDirectory){
            dir = psi
        }else{
            return false
        }

        val project = context.getData(CommonDataKeys.PROJECT) ?: return false
        val underSourceRootOfType = ProjectRootManager.getInstance(project).fileIndex.isUnderSourceRootOfType(
            dir.virtualFile,
            setOf(JavaResourceRootType.RESOURCE)
        )
        val mcVersion = MinecraftFacet.getInstance(module, McpModuleType)?.getSettings()
            ?.minecraftVersion?.let(SemanticVersion::parse)

        return underSourceRootOfType && findModid(dir, module) != null && mcVersion != null && mcVersion >= MinecraftVersions.MC1_21
    }

    private fun findModid(dir: PsiDirectory, module: Module): String? {
        val modids = MinecraftFacet.getInstance(module, ForgeModuleType)?.modIds ?: MinecraftFacet.getInstance(
            module,
            FabricModuleType
        )?.modIds ?: MinecraftFacet.getInstance(module, NeoForgeModuleType)?.modIds ?: return null

        return modids.firstOrNull { dir.virtualFile.path.split("/").contains(it) }
    }

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder.setTitle(Const.CAPTION)
        builder.setValidator(ClassInputValidator(project, directory))
        val icon = PlatformAssets.MINECRAFT_ICON
        builder.addKind("Enchantment", icon, MinecraftTemplates.JSON_ENCHANTMENT_TEMPLATE)
    }

    override fun getActionName(
        directory: PsiDirectory?,
        newName: @NonNls String,
        templateName: @NonNls String?
    ): @NlsContexts.Command String = Const.CAPTION

    override fun createFile(name: String?, templateName: String?, dir: PsiDirectory?): PsiFile? {
        val module = dir?.findModule() ?: return null
        val modid = findModid(dir, module) ?: return null

        val template = FileTemplateManager.getInstance(dir.project)
            .getInternalTemplate(templateName ?: return null)
        return createFileFromTemplate(
            name,
            template,
            dir,
            defaultTemplateProperty,
            true,
            Collections.emptyMap(),
            mapOf(Pair("MODID", modid))
        )
    }

    private object Const {
        val CAPTION
            get() = MCDevBundle("generate.json.caption")
    }
}
