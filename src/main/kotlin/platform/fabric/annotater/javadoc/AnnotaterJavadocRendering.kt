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

import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckout
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckoutService
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterSettings
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.daemon.impl.analysis.HighlightNamesUtil
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiJavaDocumentedElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.text.CharArrayUtil

/**
 * Renders annotater Javadocs with the IDE's own Java documentation renderer, by rendering the declaration in a copy of
 * its source file that has the annotater Javadoc as its doc comment.
 */
object AnnotaterJavadocRendering {
    fun checkoutForDisplay(member: PsiMember): AnnotaterCheckout? {
        if (!AnnotaterSettings.instance.showAnnotaterJavadocs) {
            return null
        }
        return AnnotaterCheckoutService.instance.checkoutFor(member)
    }

    /**
     * The offset that the annotater Javadoc of [member] is displayed above: its declaration, after any existing
     * doc comment.
     */
    fun displayOffset(member: PsiMember): Int {
        val existing = if (member is PsiCompiledElement) null else (member as? PsiJavaDocumentedElement)?.docComment
        return existing?.let { PsiTreeUtil.skipWhitespacesAndCommentsForward(it) }?.textRange?.startOffset
            ?: member.textRange.startOffset
    }

    private fun copiedDeclaration(member: PsiMember, javadoc: String?): PsiJavaDocumentedElement? {
        val target = AnnotaterJavadocTarget.fromPsi(member) ?: return null
        val copy = AnnotaterJavadocSourceCopy.create(member) { if (it == target) javadoc else null } ?: return null
        val file = copy.createPsiFile(member.project) ?: return null
        return copy.memberOf(file, target) as? PsiJavaDocumentedElement
    }

    fun generateDoc(member: PsiMember, javadoc: String?): String? {
        val declaration = copiedDeclaration(member, javadoc) ?: return null
        return JavaDocumentationProvider.generateDocStatic(declaration, null)
    }

    class LineHighlight(val line: Int, val startColumn: Int, val endColumn: Int, val attributes: TextAttributes)

    /**
     * Highlights the names that the annotater Javadocs in the top level class of [element] refer to, such as
     * parameters and linked methods, in the same way as the editor does in doc comments. The editor only highlights
     * these after resolving them, so the lexer can't.
     */
    fun highlightReferences(
        element: PsiMember,
        javadoc: (AnnotaterJavadocTarget) -> String?,
        scheme: EditorColorsScheme,
    ): Map<AnnotaterJavadocTarget, List<LineHighlight>> {
        val copy = AnnotaterJavadocSourceCopy.create(element, javadoc = javadoc) ?: return emptyMap()
        val file = copy.createPsiFile(element.project) ?: return emptyMap()
        val comments = AnnotaterJavadocTarget.declarations(file).zip(copy.targets).mapNotNull { (member, target) ->
            val comment = (member as PsiJavaDocumentedElement).docComment
            if (target != null && comment != null) target to comment else null
        }

        val holder = object : HighlightInfoHolder(file) {
            override fun getColorsScheme() = scheme
        }
        HighlightNamesUtil.highlight(file, holder) { visitor ->
            for ((_, comment) in comments) {
                PsiTreeUtil.processElements(comment) { it.accept(visitor); true }
            }
        }

        val text = file.viewProvider.contents
        val highlights = mutableMapOf<AnnotaterJavadocTarget, MutableList<LineHighlight>>()
        for (i in 0 until holder.size()) {
            val info = holder.get(i)
            val attributes = info.getTextAttributes(null, scheme)?.takeUnless { it.isEmpty } ?: continue
            val (target, comment) = comments.firstOrNull { it.second.textRange.containsRange(info.startOffset, info.endOffset) } ?: continue
            if (text.subSequence(info.startOffset, info.endOffset).contains('\n')) {
                continue
            }
            val line = text.subSequence(comment.textRange.startOffset, info.startOffset).count { it == '\n' }
            // Each line of the comment starts with its indentation, except the first, which starts after it
            val lineStart = text.lastIndexOf('\n', info.startOffset - 1) + 1
            val contentStart = CharArrayUtil.shiftForward(text, lineStart, " \t")
            highlights.getOrPut(target, ::mutableListOf) += LineHighlight(
                line,
                info.startOffset - contentStart,
                info.endOffset - contentStart,
                attributes,
            )
        }
        return highlights
    }
}
