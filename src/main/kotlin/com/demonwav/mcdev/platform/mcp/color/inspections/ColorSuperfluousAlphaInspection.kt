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

class ColorSuperfluousAlphaInspection : BaseInspection() {
    @Nls
    override fun getDisplayName(): String {
        return "MCP Color superfluous alpha component"
    }

    override fun buildErrorString(vararg infos: Any): String {
        return "This method does not expect an alpha component"
    }

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val result = infos[0] as? McpColorResult<McpColorWarning> ?: return null
        return object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                val color = result.param.extractColor(result) ?: return
                result.param.setColor(result.withArg(Color(0xFFFFFF and color.rgb, false)))
            }

            @Nls
            override fun getName() = "Remove alpha component"

            @Nls
            override fun getFamilyName() = "MCP Colors"
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitCallExpression(call: PsiCallExpression) {
                val results = McpColorMethods[call].flatMap { it.validateCall(call) }
                for (result in results) {
                    if (result.arg == McpColorWarning.SuperfluousAlpha) {
                        registerError(result.expression, result)
                    }
                }
            }
        }
    }
}
