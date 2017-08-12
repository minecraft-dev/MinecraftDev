/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.sided

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import java.io.Serializable

data class SideState(var side: Side, var reasonPointer: String?, var reason: SideReason?) : Serializable {
    constructor(side: Side, reasonPointer: PsiElement?, reason: SideReason?) : this(side, toInternalIdentifier(reasonPointer), reason)

    fun computeReason(project: Project): PsiElement? {
        return parseInternalIdentifier(reasonPointer ?: return null, project, GlobalSearchScope.allScope(project))
    }
}
