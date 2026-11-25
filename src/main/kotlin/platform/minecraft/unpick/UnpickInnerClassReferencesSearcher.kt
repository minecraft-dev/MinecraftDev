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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickClassName
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.RequestResultProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType
import com.intellij.util.Processor

/**
 * Finds references to nested classes in unpick files. Class names in unpick files are binary names such as
 * `Outer$Inner`, in which the default search doesn't find the name of the nested class as a separate word. Instead,
 * this searches for the name of the top level class, which is followed by a `$`.
 */
class UnpickInnerClassReferencesSearcher :
    QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(params: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val target = params.elementToSearch as? PsiClass ?: return
        val topLevelClass = PsiUtil.getTopLevelClass(target) ?: return
        if (topLevelClass == target) {
            return
        }
        val topLevelName = topLevelClass.name ?: return

        val scope = params.effectiveSearchScope.let { scope ->
            if (scope is GlobalSearchScope) {
                GlobalSearchScope.getScopeRestrictedByFileTypes(scope, UnpickFileType)
            } else {
                scope
            }
        }
        params.optimizer.searchWord(
            topLevelName,
            scope,
            UsageSearchContext.IN_CODE,
            true,
            target,
            object : RequestResultProcessor() {
                override fun processTextOccurrence(
                    element: PsiElement,
                    offsetInElement: Int,
                    consumer: Processor<in PsiReference>,
                ): Boolean {
                    val className = element.parentOfType<UnpickClassName>(withSelf = true) ?: return true
                    for (reference in className.references) {
                        if (reference.isReferenceTo(target) && !consumer.process(reference)) {
                            return false
                        }
                    }
                    return true
                }
            },
        )
    }
}
