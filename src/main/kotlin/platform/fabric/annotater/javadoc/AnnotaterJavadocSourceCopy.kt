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
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiJavaDocumentedElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.compiled.ClsElementImpl
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.testFramework.LightVirtualFile

/**
 * A copy of the source file of a Minecraft class (its attached sources, or otherwise its decompiled text), with the
 * existing Javadocs replaced by the annotater Javadocs as Markdown doc comments. This lets the IDE's own Java support
 * be used to edit and render annotater Javadocs in their real context.
 *
 * The member at index `i` of [members] documents `targets[i]`, or nothing if that is null, e.g. for members of other
 * top level classes in the same file.
 */
class AnnotaterJavadocSourceCopy(
    val fileName: String,
    val text: String,
    val targets: List<AnnotaterJavadocTarget?>,
) {
    /**
     * The internal name of the top level class whose Javadocs this copy edits.
     */
    val outerClass: String
        get() = targets.firstNotNullOf { it }.outerClass

    fun createPsiFile(project: Project): PsiJavaFile? {
        return PsiManager.getInstance(project).findFile(createVirtualFile()) as? PsiJavaFile
    }

    fun createVirtualFile(): LightVirtualFile = LightVirtualFile(fileName, JavaFileType.INSTANCE, text).also {
        // Markdown doc comments are only parsed at a high enough language level
        it.putUserData(PsiUtil.FILE_LANGUAGE_LEVEL_KEY, LanguageLevel.HIGHEST)
    }

    /**
     * Finds the member of [file], which must have been created from this copy, that documents [target].
     */
    fun memberOf(file: PsiJavaFile, target: AnnotaterJavadocTarget): PsiMember? {
        val index = targets.indexOf(target)
        return if (index < 0) null else members(file).getOrNull(index)
    }

    /**
     * Finds the target documented by [member] of [file], which must have been created from this copy.
     */
    fun targetOf(file: PsiJavaFile, member: PsiMember): AnnotaterJavadocTarget? {
        val index = members(file).indexOf(member)
        return if (index < 0) null else targets.getOrNull(index)
    }

    sealed class ExtractResult {
        data class Success(val javadocs: Map<AnnotaterJavadocTarget, String?>) : ExtractResult()
        data class Error(val message: String) : ExtractResult()
    }

    /**
     * Reads the Javadoc of every target back out of [file], which must have been created from this copy.
     *
     * @param isOriginal whether a range of [file] is part of the source that the user can't edit, so that comments
     * there are allowed
     */
    fun extractJavadocs(file: PsiJavaFile, isOriginal: (TextRange) -> Boolean = { false }): ExtractResult {
        val comments = PsiTreeUtil.collectElementsOfType(file, PsiComment::class.java)
        for (comment in comments) {
            if (isOriginal(comment.textRange)) {
                continue
            }
            if (comment is PsiDocComment) {
                if (!comment.isMarkdownComment) {
                    return ExtractResult.Error(MCDevBundle("fabric.annotater.javadoc.error.not_markdown"))
                }
            } else if (PsiTreeUtil.getParentOfType(comment, PsiDocComment::class.java) == null) {
                return ExtractResult.Error(MCDevBundle("fabric.annotater.javadoc.error.plain_comment"))
            }
        }

        val members = members(file)
        if (members.size != targets.size) {
            return ExtractResult.Error(MCDevBundle("fabric.annotater.javadoc.error.structure_changed"))
        }
        val javadocs = LinkedHashMap<AnnotaterJavadocTarget, String?>()
        for ((member, target) in members.zip(targets)) {
            if (target != null) {
                javadocs[target] = (member as PsiJavaDocumentedElement).docComment?.let(::extractMarkdown)
            }
        }
        return ExtractResult.Success(javadocs)
    }

    companion object {
        /**
         * Finds the classes, fields and methods declared in [file], in the order matching [targets]. Local and
         * anonymous classes are not included, and neither are members that don't appear in the source, such as the
         * implicit methods of records.
         */
        fun members(file: PsiJavaFile): List<PsiMember> = members(file.classes).toList()

        private fun members(classes: Array<PsiClass>): Sequence<PsiMember> = classes.asSequence().flatMap { clazz ->
            sequenceOf(clazz) + clazz.fields + clazz.methods + members(clazz.innerClasses)
        }.filter { it.node != null }

        /**
         * Extracts the Markdown text of a `///` doc comment, removing the `///` prefixes and the common indentation.
         * Returns null when the comment is blank.
         */
        fun extractMarkdown(comment: PsiDocComment): String? {
            val lines = comment.text.lines().map { line ->
                val trimmed = line.trimStart()
                if (trimmed.startsWith("///")) trimmed.substring(3) else trimmed
            }
            val indent = lines.filter { it.isNotBlank() }.minOfOrNull { line -> line.takeWhile { it == ' ' }.length }
                ?: return null
            val text = lines.joinToString("\n") { if (it.isBlank()) "" else it.substring(indent).trimEnd() }
                .trim('\n')
            return text.ifEmpty { null }
        }

        /**
         * Formats Markdown as a `///` doc comment with the given indentation, ending with a newline.
         */
        fun formatMarkdown(markdown: String, indent: String): String {
            return markdown.lines().joinToString("") { line ->
                if (line.isEmpty()) "$indent///\n" else "$indent/// $line\n"
            }
        }

        /**
         * Formats Markdown as a `///` doc comment to insert at a column after [indent], followed by [indent] again for
         * the declaration after it.
         */
        fun formatInsertedMarkdown(markdown: String, indent: String): String {
            // Leave a space after an empty first line, so the user can start typing there straight away
            return "/// " + formatMarkdown(markdown, indent).removePrefix("$indent///").removePrefix(" ") + indent
        }

        /**
         * The source file that [clazz] is shown in: its attached sources, or otherwise its decompiled text.
         */
        private fun sourceFile(clazz: PsiClass): PsiJavaFile? {
            val file = clazz.navigationElement.containingFile
            return (if (file is PsiCompiledElement) file.mirror else file) as? PsiJavaFile
        }

        /**
         * Copies the source file of the top level class containing [element], a class, field or method.
         *
         * @param placeholderFor a target to add an empty doc comment to if it doesn't have an annotater Javadoc, so
         * that the user can start typing one straight away
         * @param javadoc looks up the current annotater Javadoc of a target
         */
        fun create(
            element: PsiMember,
            placeholderFor: AnnotaterJavadocTarget? = null,
            javadoc: (AnnotaterJavadocTarget) -> String?,
        ): AnnotaterJavadocSourceCopy? {
            val outermost = generateSequence(element as? PsiClass ?: element.containingClass) { it.containingClass }
                .lastOrNull() ?: return null
            val outerClass = AnnotaterJavadocTarget.fromPsi(outermost)?.outerClass ?: return null
            val source = sourceFile(outermost) ?: return null
            val text = source.text

            val targets = mutableListOf<AnnotaterJavadocTarget?>()
            // The replacements of the existing doc comments, in text order
            val replacements = mutableListOf<Pair<TextRange, String>>()
            for (member in members(source)) {
                // Decompiled text is generated from the compiled class, which is more reliable to get the target of
                val compiled = member.getUserData(ClsElementImpl.COMPILED_ELEMENT) ?: member
                val target = AnnotaterJavadocTarget.fromPsi(compiled)?.takeIf { it.outerClass == outerClass }
                val existing = (member as PsiJavaDocumentedElement).docComment
                val start = existing?.textRange?.startOffset ?: member.textRange.startOffset
                val lineStart = text.lastIndexOf('\n', start - 1) + 1
                val indent = text.substring(lineStart, start)
                // Doc comments can only be inserted above members that start their own line
                if (target == null || indent.isNotBlank()) {
                    targets += null
                    continue
                }
                targets += target

                val markdown = javadoc(target) ?: "".takeIf { target == placeholderFor }
                val end = existing?.let { (it.nextSibling as? PsiWhiteSpace ?: it).textRange.endOffset } ?: start
                if (markdown != null || existing != null) {
                    val replacement = markdown?.let { formatInsertedMarkdown(it, indent) }.orEmpty()
                    replacements += TextRange(start, end) to replacement
                }
            }
            if (targets.all { it == null }) {
                return null
            }

            val sb = StringBuilder(text)
            for ((range, replacement) in replacements.sortedByDescending { it.first.startOffset }) {
                sb.replace(range.startOffset, range.endOffset, replacement)
            }
            return AnnotaterJavadocSourceCopy("${outermost.name}.java", sb.toString(), targets)
        }
    }
}
