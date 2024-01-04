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

package com.demonwav.mcdev.platform.mixin.config.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.util.reference.ClassNameReferenceProvider
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PackageScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

object MixinClass : ClassNameReferenceProvider() {

    override fun getBasePackage(element: PsiElement): String? {
        // Literal -> Array -> Property -> Object
        val obj = element.parent?.parent?.parent as? JsonObject ?: return null
        return (obj.findProperty("package")?.value as? JsonStringLiteral)?.value
    }

    override fun findClasses(element: PsiElement, scope: GlobalSearchScope): List<PsiClass> {
        val facade = JavaPsiFacade.getInstance(element.project)
        val mixinAnnotation = facade.findClass(MIXIN, element.resolveScope) ?: return emptyList()

        val packageScope = getBasePackage(element)?.let { facade.findPackage(it) }
            ?.let { scope.intersectWith(PackageScope(it, true, true)) } ?: scope
        return AnnotatedElementsSearch.searchPsiClasses(mixinAnnotation, packageScope).toList()
    }
}
