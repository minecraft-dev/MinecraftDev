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

package com.demonwav.mcdev.translations.reference

import com.demonwav.mcdev.translations.TranslationFiles
import com.intellij.find.FindModel
import com.intellij.find.impl.FindInProjectUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.createSmartPointer
import com.intellij.usages.FindUsagesProcessPresentation
import com.intellij.usages.UsageViewPresentation
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.intellij.util.application

class TranslationReferenceSearch : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>): Boolean {
        if (application.isReadAccessAllowed) {
            // Skip when we're in a read action because FindInProjectUtil.findUsages forbids it
            // I am unsure why we are in a read action sometimes, thankfully references still seem to resolve correctly
            return true
        }

        val entryPointer = parameters.elementToSearch.createSmartPointer()

        val key = runReadAction { entryPointer.element?.let(TranslationFiles::toTranslation)?.key } ?: return true

        fun <A> power(start: List<A>): Set<List<A>> {
            tailrec fun pwr(s: List<A>, acc: Set<List<A>>): Set<List<A>> =
                if (s.isEmpty()) {
                    acc
                } else {
                    pwr(s.takeLast(s.size - 1), acc + acc.map { it + s.first() })
                }
            return pwr(start, setOf(emptyList()))
        }

        val model = FindModel()
        model.customScope = parameters.effectiveSearchScope
        model.isCaseSensitive = true
        model.searchContext = FindModel.SearchContext.IN_STRING_LITERALS
        model.isRegularExpressions = true
        // Enables custom translations functions (for auto-prefixing calls, for instance)
        model.stringToFind = power(key.split('.'))
            .map { it.joinToString(".") }
            .filter { it.isNotEmpty() }
            .joinToString("|") { "(${Regex.escape(it)})" }

        FindInProjectUtil.findUsages(
            model,
            parameters.project,
            {
                if (it.file != null && it.element != null && it.rangeInElement != null) {
                    val highlighted = it.file?.findElementAt(it.rangeInElement!!.startOffset)
                    val ref = highlighted?.parent?.references
                        ?.find { ref -> ref is TranslationReference } as TranslationReference?
                    if (ref?.key?.full == key) {
                        consumer.process(ref)
                    }
                }
                true
            },
            FindUsagesProcessPresentation(UsageViewPresentation()),
        )
        return true
    }
}
