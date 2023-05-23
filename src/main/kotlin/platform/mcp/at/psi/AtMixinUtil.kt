/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
