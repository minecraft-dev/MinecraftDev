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

package com.demonwav.mcdev.platform.mixin.expression.reference

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.RequestResultProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import com.intellij.util.Processor

class MEDefinitionReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val targetElement = queryParameters.elementToSearch

        if (!MEReferenceUtil.isDefinitionId(targetElement)) {
            return
        }

        val strValue = targetElement.constantStringValue ?: return
        queryParameters.optimizer.searchWord(
            strValue,
            queryParameters.effectiveSearchScope,
            UsageSearchContext.IN_STRINGS,
            true,
            targetElement,
            object : RequestResultProcessor() {
                override fun processTextOccurrence(
                    element: PsiElement,
                    offsetInElement: Int,
                    consumer: Processor<in PsiReference>
                ): Boolean {
                    val meElement = InjectedLanguageManager.getInstance(queryParameters.project).findInjectedElementAt(
                        element.containingFile,
                        element.textOffset + offsetInElement
                    ) ?: return false
                    val meName = meElement.parentOfType<MEName>(withSelf = true) ?: return false
                    for (reference in meName.references) {
                        if (reference.isReferenceTo(targetElement)) {
                            if (!consumer.process(reference)) {
                                return true
                            }
                        }
                    }
                    return false
                }
            },
        )
    }
}
