/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

fun callbackInfoType(project: Project): PsiType? = PsiType.getTypeByName(CALLBACK_INFO, project, GlobalSearchScope.allScope(project))

fun callbackInfoReturnableType(project: Project, returnType: PsiType): PsiType? {
    // TODO: Can we do this without looking up the PsiClass?
    val psiClass = JavaPsiFacade.getInstance(project).findClass(CALLBACK_INFO_RETURNABLE, GlobalSearchScope.allScope(project)) ?: return null
    return JavaPsiFacade.getElementFactory(project).createType(psiClass, returnType)
}
