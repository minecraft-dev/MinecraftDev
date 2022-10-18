/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.reference

import com.demonwav.mcdev.translations.TranslationFiles
import com.intellij.find.FindModel
import com.intellij.find.impl.FindInProjectUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usages.FindUsagesProcessPresentation
import com.intellij.usages.UsageViewPresentation
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor

class TranslationReferenceSearch : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>): Boolean {
        val entry = parameters.elementToSearch

        val key = runReadAction { TranslationFiles.toTranslation(entry)?.key } ?: return true

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

        runReadAction {
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
                FindUsagesProcessPresentation(UsageViewPresentation())
            )
        }
        return true
    }
}
