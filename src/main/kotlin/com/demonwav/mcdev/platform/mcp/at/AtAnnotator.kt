/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.at.psi.AtElement
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import java.awt.Font

class AtAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is AtEntry) {
            return
        }

        val member = element.function ?: element.fieldName ?: return

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val facet = MinecraftFacet.getInstance(module) ?: return
        val mcpModule = facet.getModuleOfType(McpModuleType) ?: return
        val srgMap = mcpModule.srgManager?.srgMapNow ?: return

        val reference = AtMemberReference.get(element, member) ?: return

        // We can resolve this without checking the srg map
        if (reference.resolveMember(element.project) != null) {
            if (member is AtFunction) {
                underline(member.funcName, holder)
            } else {
                underline(member, holder)
            }
            return
        }

        // We need to check the srg map, or it can't be resolved (and no underline)
        when (member) {
            is AtFieldName -> {
                srgMap.getMcpField(reference)?.resolveMember(element.project) ?: return
                underline(member, holder)
            }
            is AtFunction -> {
                srgMap.getMcpMethod(reference)?.resolveMember(element.project) ?: return
                underline(member.funcName, holder)
            }
        }
    }

    private fun underline(element: AtElement, holder: AnnotationHolder) {
        val annotation = holder.createInfoAnnotation(element, null)
        annotation.enforcedTextAttributes =
            TextAttributes(
                null,
                null,
                AtSyntaxHighlighter.ELEMENT_NAME.defaultAttributes.foregroundColor,
                EffectType.BOLD_LINE_UNDERSCORE,
                Font.PLAIN
            )
    }
}
