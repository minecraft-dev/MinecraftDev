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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.at.psi.AtElement
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
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
        val srgMap = mcpModule.mappingsManager?.mappingsNow ?: return

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
                srgMap.tryGetMappedField(reference)?.resolveMember(element.project) ?: return
                underline(member, holder)
            }
            is AtFunction -> {
                srgMap.tryGetMappedMethod(reference)?.resolveMember(element.project) ?: return
                underline(member.funcName, holder)
            }
        }
    }

    private fun underline(element: AtElement, holder: AnnotationHolder) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .enforcedTextAttributes(
                TextAttributes(
                    null,
                    null,
                    AtSyntaxHighlighter.ELEMENT_NAME.defaultAttributes.foregroundColor,
                    EffectType.BOLD_LINE_UNDERSCORE,
                    Font.PLAIN,
                ),
            )
            .create()
    }
}
