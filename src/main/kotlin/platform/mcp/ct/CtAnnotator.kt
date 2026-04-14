/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.ct

import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtAccess
import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtClassLiteral
import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtExtendEnumEntry
import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtFieldLiteral
import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtHeader
import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtItfEntry
import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtMethodLiteral
import com.demonwav.mcdev.util.childOfType
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimaps
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

class CtAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val effectiveVersion by lazy { element.containingFile?.childOfType<CtHeader>()?.effectiveVersion ?: 1 }

        when (element) {
            is CtAccess -> {
                val access = element.text
                val target = PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java)?.text
                if (!TokenSets.compatibleByAccessMap.get(access).contains(target)) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Access '$access' cannot be used on '$target'").create()
                }

                if (element.accessElement.text.startsWith("transitive-") && effectiveVersion < 2) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Transitive accesses were introduced in v2").create()
                }
            }
            is CtItfEntry -> {
                if (effectiveVersion < 3) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Interface injection was introduced in ClassTweaker v1")
                        .range(element.firstChild)
                        .create()
                }
            }
            is CtExtendEnumEntry -> {
                if (effectiveVersion < 4) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Enum extension was introduced in ClassTweaker v2")
                        .range(element.firstChild)
                        .create()
                }
            }
            is CtFieldLiteral, is CtMethodLiteral, is CtClassLiteral -> {
                val target = element.text
                val access = PsiTreeUtil.skipSiblingsBackward(element, PsiWhiteSpace::class.java)?.text
                if (!TokenSets.compatibleByTargetMap.get(target).contains(access)) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "'$target' cannot be used with '$access'").create()
                }
            }
            is CtHeader -> {
                val version = element.effectiveVersion
                val versionElement = element.versionElement ?: return
                when (element.nameString) {
                    "accessWidener" -> {
                        if (version == null || version < 1 || version > 2) {
                            holder.newAnnotation(HighlightSeverity.ERROR, "Unrecognized access widener version").range(versionElement).create()
                        }
                    }
                    "classTweaker" -> {
                        if (version == null || version < 3 || version > FabricConstants.CLASS_TWEAKER_VERSION) {
                            holder.newAnnotation(HighlightSeverity.ERROR, "Unrecognized class tweaker version").range(versionElement).create()
                        }
                    }
                }
            }
        }
    }

    object TokenSets {
        val compatibleByAccessMap = HashMultimap.create<String, String>()
        val compatibleByTargetMap = HashMultimap.create<String, String>()

        init {
            compatibleByAccessMap.putAll("accessible", setOf("class", "method", "field"))
            compatibleByAccessMap.putAll("transitive-accessible", setOf("class", "method", "field"))
            compatibleByAccessMap.putAll("extendable", setOf("class", "method"))
            compatibleByAccessMap.putAll("transitive-extendable", setOf("class", "method"))
            compatibleByAccessMap.putAll("mutable", setOf("field"))
            compatibleByAccessMap.putAll("transitive-mutable", setOf("field"))
            Multimaps.invertFrom(compatibleByAccessMap, compatibleByTargetMap)
        }
    }
}
