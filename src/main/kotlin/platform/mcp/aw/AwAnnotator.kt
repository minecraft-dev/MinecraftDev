/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwAccess
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwClassLiteral
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwFieldLiteral
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwHeader
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwMethodLiteral
import com.demonwav.mcdev.util.childOfType
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimaps
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

class AwAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element is AwAccess) {
            val access = element.text
            val target = PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java)?.text
            if (!compatibleByAccessMap.get(access).contains(target)) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Access '$access' cannot be used on '$target'").create()
            }

            if (element.accessElement.text.startsWith("transitive-") &&
                element.containingFile?.childOfType<AwHeader>()?.versionString == "v1"
            ) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Transitive accesses were introduced in v2").create()
            }
        } else if (element is AwFieldLiteral || element is AwMethodLiteral || element is AwClassLiteral) {
            val target = element.text
            val access = PsiTreeUtil.skipSiblingsBackward(element, PsiWhiteSpace::class.java)?.text
            if (!compatibleByTargetMap.get(target).contains(access)) {
                holder.newAnnotation(HighlightSeverity.ERROR, "'$target' cannot be used with '$access'").create()
            }
        }
    }

    companion object {

        val compatibleByAccessMap = HashMultimap.create<String, String>()!!
        val compatibleByTargetMap = HashMultimap.create<String, String>()!!

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
