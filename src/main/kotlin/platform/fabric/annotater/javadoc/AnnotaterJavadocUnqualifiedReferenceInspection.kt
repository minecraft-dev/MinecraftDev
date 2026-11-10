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

package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil

/**
 * Annotater requires classes in Javadoc to be referenced by their fully qualified name.
 */
class AnnotaterJavadocUnqualifiedReferenceInspection : LocalInspectionTool() {
    override fun getStaticDescription() = MCDevBundle("fabric.annotater.inspection.unqualified.description")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (AnnotaterJavadocVirtualFile.of(holder.file) == null) {
            return PsiElementVisitor.EMPTY_VISITOR
        }
        return object : JavaElementVisitor() {
            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
                // Only check whole references, not their qualifiers
                if (reference.parent is PsiJavaCodeReferenceElement) {
                    return
                }
                if (PsiTreeUtil.getParentOfType(reference, PsiDocComment::class.java) == null) {
                    return
                }
                val qualifiedName = qualifiedNameIfUnqualified(reference) ?: return
                holder.registerProblem(
                    reference,
                    MCDevBundle("fabric.annotater.inspection.unqualified.problem", qualifiedName),
                    QualifyFix(qualifiedName),
                )
            }
        }
    }

    private class QualifyFix(private val qualifiedName: String) : LocalQuickFix {
        override fun getName() = MCDevBundle("fabric.annotater.inspection.unqualified.fix", qualifiedName)

        override fun getFamilyName() = MCDevBundle("fabric.annotater.inspection.unqualified.fix.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val reference = descriptor.psiElement as? PsiJavaCodeReferenceElement ?: return
            val file = reference.containingFile ?: return
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return
            val range = reference.textRange
            document.replaceString(range.startOffset, range.endOffset, qualifiedName)
            documentManager.commitDocument(document)
        }
    }

    companion object {
        /**
         * Returns the fully qualified name of the class [reference] refers to, if it isn't already written as such.
         */
        fun qualifiedNameIfUnqualified(reference: PsiJavaCodeReferenceElement): String? {
            val resolved = reference.resolve() as? PsiClass ?: return null
            val qualifiedName = resolved.qualifiedName ?: return null
            val written = reference.text.filterNot { it.isWhitespace() }
            return qualifiedName.takeIf { written != it }
        }
    }
}
