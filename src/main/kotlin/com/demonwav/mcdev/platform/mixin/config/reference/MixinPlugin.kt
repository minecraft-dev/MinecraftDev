/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.MIXIN_PLUGIN
import com.demonwav.mcdev.util.reference.ClassNameReferenceProvider
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiUtil

object MixinPlugin : ClassNameReferenceProvider() {

    fun findInterface(context: PsiElement): PsiClass? {
        return JavaPsiFacade.getInstance(context.project).findClass(MIXIN_PLUGIN, context.resolveScope)
    }

    override fun findClasses(element: PsiElement, scope: GlobalSearchScope): List<PsiClass> {
        val configInterface = findInterface(element) ?: return emptyList()
        return ClassInheritorsSearch.search(configInterface, scope, true, true, false).filter {
            PsiUtil.isInstantiatable(it)
        }
    }
}
