/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.color.inspections

import com.demonwav.mcdev.platform.mcp.color.McpColorMethod
import com.demonwav.mcdev.platform.mcp.color.McpColorMethods
import com.demonwav.mcdev.platform.mcp.color.McpColorResult
import com.demonwav.mcdev.platform.mcp.color.McpColorWarning
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCallExpression
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls
import java.awt.Color

class MissingColorComponentInspection : BaseInspection() {
    @Nls
    override fun getDisplayName(): String {
        return "MCP Color missing one or more component"
    }

    override fun buildErrorString(vararg infos: Any): String {
        return "Color missing ${(infos[1] as List<String>).joinToString(" and ")} component (implied as zero)"
    }

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val result = infos[0] as? McpColorResult<McpColorWarning> ?: return null
        return object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                val color = result.param.extractColor(result) ?: return
                val newColor = if (!result.param.hasAlpha) 0xFFFFFF and color.rgb else color.rgb
                result.param.setColor(result.withArg(Color(newColor, result.param.hasAlpha)))
            }

            @Nls
            override fun getName() = "Pad color with zero components"

            @Nls
            override fun getFamilyName() = "MCP Colors"
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitCallExpression(call: PsiCallExpression) {
                val results = McpColorMethods[call].flatMap { it.validateCall(call) }
                for (result in results) {
                    if (result.arg is McpColorWarning.MissingComponents) {
                        registerError(result.expression, result, result.arg.components)
                    }
                }
            }
        }
    }
}
