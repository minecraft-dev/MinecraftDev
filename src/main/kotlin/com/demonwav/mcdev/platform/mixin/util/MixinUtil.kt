/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("MixinUtil")
package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.Contract

@Contract(pure = true)
internal fun callbackInfoType(project: Project): PsiType? = PsiType.getTypeByName(CALLBACK_INFO, project, GlobalSearchScope.allScope(project))

@Contract(pure = true)
internal fun callbackInfoReturnableType(project: Project, context: PsiElement, returnType: PsiType): PsiType? {
    val boxedType = if (returnType is PsiPrimitiveType) returnType.getBoxedType(context)!! else returnType

    // TODO: Can we do this without looking up the PsiClass?
    val psiClass = JavaPsiFacade.getInstance(project).findClass(CALLBACK_INFO_RETURNABLE, GlobalSearchScope.allScope(project)) ?: return null
    return JavaPsiFacade.getElementFactory(project).createType(psiClass, boxedType)
}
