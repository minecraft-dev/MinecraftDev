/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch

class AtUsageInspection : LocalInspectionTool() {

    override fun getStaticDescription(): String? {
        return "The declared access transformer is never used"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is AtEntry) {
                    return
                }

                val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
                val instance = MinecraftFacet.getInstance(module) ?: return
                val mcpModule = instance.getModuleOfType(McpModuleType) ?: return
                val srgMap = mcpModule.srgManager?.srgMapNow ?: return

                val member = element.function ?: element.fieldName ?: return
                val reference = AtMemberReference.get(element, member) ?: return

                val psi = when (member) {
                    is AtFunction ->
                        reference.resolveMember(element.project) ?: srgMap.getMcpMethod(reference)?.resolveMember(
                            element.project
                        ) ?: return
                    is AtFieldName ->
                        reference.resolveMember(element.project)
                            ?: srgMap.getMcpField(reference)?.resolveMember(element.project) ?: return
                    else ->
                        return
                }

                val query = ReferencesSearch.search(psi, GlobalSearchScope.projectScope(element.project))
                query.findFirst()
                    ?: holder.registerProblem(
                        element,
                        "Access Transformer entry is never used",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                    )
            }
        }
    }
}
