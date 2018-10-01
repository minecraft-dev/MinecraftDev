/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.color.inspections

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.color.McpColorMethod
import com.demonwav.mcdev.platform.mcp.color.McpColorMethods
import com.demonwav.mcdev.platform.mcp.color.McpColorResult
import com.demonwav.mcdev.platform.mcp.color.McpColorWarning
import com.demonwav.mcdev.platform.mcp.color.findColors
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCallExpression
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class ColorComponentOutOfRangeInspection : BaseInspection() {
    @Nls
    override fun getDisplayName(): String {
        return "MCP Color component out of range"
    }

    override fun buildErrorString(vararg infos: Any): String {
        return "Color component is out of [${infos[1]},${infos[2]}] range, this can lead to unexpected behavior."
    }

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val result = infos[0] as? McpColorResult<McpColorWarning> ?: return null
        val clamp = infos[3] as? (McpColorResult<Any>) -> Unit ?: return null
        return object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                clamp(result)
            }

            @Nls
            override fun getName() = "Clamp value to range"

            @Nls
            override fun getFamilyName() = "MCP Colors"
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitCallExpression(call: PsiCallExpression) {
                val results = McpColorMethods[call].flatMap { it.validateCall(call) }
                for (result in results) {
                    if (result.arg is McpColorWarning.ComponentOutOfRange) {
                        registerError(result.expression, result, result.arg.min, result.arg.max, result.arg.clamp)
                    }
                }
            }
        }
    }
}
