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

package com.demonwav.mcdev.platform.bukkit.completion

import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiModifier
import com.intellij.psi.impl.JavaPsiFacadeEx
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.ProcessingContext

class BukkitEventHandlerCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        completionResultSet: CompletionResultSet
    ) {
        val prefix = completionResultSet.prefixMatcher.prefix
        if (!prefix.startsWith("on") || prefix.length == 2) {
            return
        }

        val position = completionParameters.position
        val containingClass = position.findContainingClass() ?: return
        val project = position.project
        val facade = JavaPsiFacadeEx.getInstance(project)

        val eventListenerClass = facade.findClass(BukkitConstants.LISTENER_CLASS, GlobalSearchScope.allScope(project)) ?: return
        if (!containingClass.isInheritor(eventListenerClass, true)) {
            return
        }

        if (position.findContainingMethod() != null) {
            return
        }

        val scope = GlobalSearchScope.allScope(project)
        val eventBaseClass = facade.findClass(BukkitConstants.EVENT_CLASS, scope) ?: return
        val eventNameFilter = prefix.substring(2).lowercase()

        ClassInheritorsSearch.search(eventBaseClass, scope, true)
            .forEach { psiClass ->
                if (psiClass.isInterface || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    return@forEach
                }

                val eventSimpleName = psiClass.name ?: return@forEach
                if (eventNameFilter.isNotEmpty() && !eventSimpleName.lowercase().startsWith(eventNameFilter)) {
                    return@forEach
                }

                val lookupString = "on$eventSimpleName"
                val qualifiedName = psiClass.qualifiedName

                val element = LookupElementBuilder
                    .create(lookupString)
                    .withPresentableText("$lookupString()")
                    .withTailText(" - $qualifiedName", true)
                    .withTypeText("@EventHandler")
                    .withIcon(AllIcons.Nodes.Method)
                    .withBaseLookupString(lookupString)
                    .withBoldness(true)
                    .withInsertHandler(BukkitEventHandlerInsertHandler(psiClass))

                completionResultSet.addElement(
                    PrioritizedLookupElement.withPriority(element, 100.0)
                )
            }
    }
}
