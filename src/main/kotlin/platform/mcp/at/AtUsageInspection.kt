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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.demonwav.mcdev.util.excludeFileTypes
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.elementType
import com.intellij.psi.util.siblings

class AtUsageInspection : LocalInspectionTool() {

    override fun getStaticDescription(): String {
        return "Reports unused Access Transformer entries"
    }

    override fun isSuppressedFor(element: PsiElement): Boolean {
        return super.isSuppressedFor(element)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is AtEntry) {
                    return
                }

                val function = element.function
                if (function != null) {
                    checkElement(element, function)
                    return
                }

                val fieldName = element.fieldName
                if (fieldName != null) {
                    checkElement(element, fieldName)
                    return
                }

                // Only check class names if it is the target of the entry
                checkElement(element, element.className)
            }

            private fun checkElement(entry: AtEntry, element: PsiElement) {
                val referenced = element.reference?.resolve() ?: return
                val scope = GlobalSearchScope.projectScope(element.project)
                    .excludeFileTypes(element.project, AtFileType)
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
                    for (childEntry in entry.containingFile.children) {
                        if (childEntry !is AtEntry || childEntry == entry) {
                            continue
                        }

                        val function = childEntry.function ?: continue
                        val otherResolved = function.reference?.resolve()
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

                val fix = RemoveAtEntryFix.forWholeLine(entry)
                holder.registerProblem(entry, "Access Transformer entry is never used", fix)
            }
        }
    }

    private class RemoveAtEntryFix(startElement: PsiElement, endElement: PsiElement) :
        LocalQuickFixOnPsiElement(startElement, endElement) {

        override fun getFamilyName(): @IntentionFamilyName String = "Remove entry"

        override fun getText(): @IntentionName String = familyName

        override fun invoke(
            project: Project,
            file: PsiFile,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            startElement.parent.deleteChildRange(startElement, endElement)
        }

        companion object {

            fun forWholeLine(entry: AtEntry): RemoveAtEntryFix {
                val start = entry.siblings(forward = false, withSelf = false)
                    .firstOrNull { it.elementType == AtTypes.CRLF }?.nextSibling
                val end = entry.siblings(forward = true, withSelf = true)
                    .firstOrNull { it.elementType == AtTypes.CRLF }
                return RemoveAtEntryFix(start ?: entry, end ?: entry)
            }
        }
    }
}
