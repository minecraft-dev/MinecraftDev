/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.platform.MinecraftModule
import com.demonwav.mcdev.platform.mcp.McpModule
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtAsterisk
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.srg.SrgManager
import com.intellij.codeHighlighting.HighlightDisplayLevel

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
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
            override fun visitElement(element: PsiElement?) {
                if (element !is AtEntry) {
                    return
                }

                val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return

                val instance = MinecraftModule.getInstance(module) ?: return

                val mcpModule = instance.getModuleOfType(McpModuleType.getInstance()) ?: return

                val psiClass = element.className.classNameValue ?: // Ignore here, we'll flag this as an error in another inspection
                    return

                SrgManager.getInstance(mcpModule).srgMap.done { srgMap ->
                    val member = if (element.function != null) {
                        element.function
                    } else if (element.fieldName != null) {
                        element.fieldName
                    } else {
                        element.asterisk
                    }

                    if (member is AtAsterisk) {
                        return@done
                    }

                    if (member is AtFunction) {
                        val methodMcp = srgMap.findMethodSrgToMcp("${element.className.classNameText}/${member.text}") ?:
                            "${element.className.classNameText}/${member.text}"
                    } else if (member is AtFieldName) {
                        val fieldMcp = srgMap.findFieldSrgToMcp("${element.className.classNameText}/${member.text}") ?:
                            "${element.className.classNameText}/${member.text}"

                        val fieldName = fieldMcp.substring(fieldMcp.lastIndexOf('/'))

                        val psiField = psiClass.findFieldByName(fieldName, false) ?: return@done

                        val query = ReferencesSearch.search(psiField, GlobalSearchScope.projectScope(element.project))
                        query.findFirst() ?: return@done

                        holder.registerProblem(element, "Access Transformer entry is never used", ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                    }
                }
            }
        }
    }
}
