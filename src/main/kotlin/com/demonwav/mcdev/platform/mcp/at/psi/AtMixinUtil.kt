/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi

import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.getPrimitiveWrapperClass
import com.demonwav.mcdev.util.parseClassDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

private val bracket = Regex("\\[")

fun getClassFromString(text: String?, project: Project): PsiClass? {
    var newText = text ?: return null

    // We don't care about arrays
    newText = newText.replace(bracket, "")

    val scope = GlobalSearchScope.allScope(project)

    if (newText.startsWith("L")) {
        val finalText = parseClassDescriptor(newText)
        return findQualifiedClass(project, finalText, scope)
    }

    if (newText.length != 1 || !"BCDFIJSZ".contains(newText[0])) {
        return null
    }

    return getPrimitiveWrapperClass(newText[0], project)
}
