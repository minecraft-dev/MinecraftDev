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
