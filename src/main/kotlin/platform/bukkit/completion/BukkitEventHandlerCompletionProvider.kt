package com.demonwav.mcdev.platform.bukkit.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.impl.JavaPsiFacadeEx
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NotNull

class BukkitEventHandlerCompletionProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        const val EVENT_LISTENER = "org.bukkit.event.Listener"
        private const val BUKKIT_EVENT_FQN = "org.bukkit.event.Event"
    }

    override fun addCompletions(
        @NotNull completionParameters: CompletionParameters,
        @NotNull processingContext: ProcessingContext,
        @NotNull completionResultSet: CompletionResultSet
    ) {
        val prefix = completionResultSet.prefixMatcher.prefix
        if (!prefix.startsWith("on") || prefix.length == 2) return

        val position = completionParameters.position
        val containingClass = PsiTreeUtil.getParentOfType(position, PsiClass::class.java) ?: return
        val project: Project = position.project
        val facade = JavaPsiFacadeEx.getInstanceEx(project)

        val eventListenerClass = facade.findClass(EVENT_LISTENER, GlobalSearchScope.allScope(project)) ?: return
        if (!containingClass.isInheritor(eventListenerClass, true)) return
        if (PsiTreeUtil.getParentOfType(position, PsiMethod::class.java) != null) return

        val scope = GlobalSearchScope.allScope(project)
        val eventBaseClass = facade.findClass(BUKKIT_EVENT_FQN, scope) ?: return

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
                val methodName = lookupString.replace("Event", "")
                val qualifiedName = psiClass.qualifiedName

                val element = LookupElementBuilder
                    .create(lookupString)
                    .withPresentableText("$lookupString()")
                    .withTailText(" - $qualifiedName", true)
                    .withTypeText("@EventHandler")
                    .withIcon(AllIcons.Nodes.Method)
                    .withBaseLookupString(lookupString)
                    .withBoldness(true)
                    .withInsertHandler(BukkitEventHandlerInsertHandler(methodName, qualifiedName))

                completionResultSet.addElement(
                    PrioritizedLookupElement.withPriority(element, 100.0)
                )
            }

        completionResultSet.stopHere()
    }
}
