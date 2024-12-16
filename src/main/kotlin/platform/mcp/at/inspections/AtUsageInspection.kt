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

package com.demonwav.mcdev.platform.mcp.at.inspections

import com.demonwav.mcdev.platform.mcp.at.AtFileType
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtVisitor
import com.demonwav.mcdev.util.excludeFileTypes
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch

class AtUsageInspection : LocalInspectionTool() {

    override fun getStaticDescription(): String {
        return "Reports unused Access Transformer entries"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : AtVisitor() {

            private val fixProvider = { it: AtEntry -> RemoveAtEntryFix.forWholeLine(it, true) }

            override fun visitEntry(entry: AtEntry) {
                val function = entry.function
                if (function != null) {
                    checkElement(entry, function, holder, AtFileType, fixProvider) { file, toSkip ->
                        file.children.asSequence()
                            .filterIsInstance<AtEntry>()
                            .filter { it != toSkip }
                            .mapNotNull { it.function?.reference }
                    }
                    return
                }

                val fieldName = entry.fieldName
                if (fieldName != null) {
                    checkElement(entry, fieldName, holder, AtFileType, fixProvider)
                    return
                }

                // Only check class names if it is the target of the entry
                checkElement(entry, entry.className, holder, AtFileType, fixProvider)
            }
        }
    }

    companion object {

        @JvmStatic
        fun <E: PsiElement> checkElement(
            entry: E,
            element: PsiElement,
            holder: ProblemsHolder,
            fileType: FileType,
            fixProvider: (entry: E) -> LocalQuickFix,
            entriesReferenceProvider: (PsiFile, toSkip: E) -> Sequence<PsiReference> = { _, _ -> emptySequence() }
        ) {
            val referenced = element.reference?.resolve() ?: return
            val scope = GlobalSearchScope.projectScope(element.project)
                .excludeFileTypes(element.project, fileType)
            val query = ReferencesSearch.search(referenced, scope, true)
            if (query.any()) {
                return
            }

            if (referenced is PsiMethod) {
                // The regular references search doesn't cover overridden methods
                val overridingQuery = OverridingMethodsSearch.search(referenced, scope, true)
                if (overridingQuery.any()) {
                    return
                }

                // Also ignore if other entries cover super methods
                val superMethods = referenced.findSuperMethods()
                for (reference in entriesReferenceProvider(entry.containingFile, entry)) {
                    val otherResolved = reference.resolve()
                    if (superMethods.contains(otherResolved)) {
                        return
                    }
                }
            }

            if (referenced is PsiClass) {
                // Do not report classes whose members are used in the mod
                for (field in referenced.fields) {
                    if (ReferencesSearch.search(field, scope, true).any()) {
                        return
                    }
                }
                for (method in referenced.methods) {
                    if (ReferencesSearch.search(method, scope, true).any()) {
                        return
                    }
                }
                for (innerClass in referenced.innerClasses) {
                    if (ReferencesSearch.search(innerClass, scope, true).any()) {
                        return
                    }
                }
            }

            holder.registerProblem(entry, "Entry is never used", fixProvider(entry))
        }
    }
}
