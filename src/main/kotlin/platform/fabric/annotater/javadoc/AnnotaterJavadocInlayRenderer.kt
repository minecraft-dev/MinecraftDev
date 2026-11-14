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

import com.intellij.ide.highlighter.JavaFileHighlighter
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiMember
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.text.CharArrayUtil
import java.awt.Graphics
import java.awt.Rectangle

/**
 * Displays an annotater Javadoc above its declaration as the `///` comment it would be in the source, highlighted in
 * the same way as a doc comment in a Java file.
 *
 * @param highlights the highlighting of the names that the Javadoc refers to, on top of the lexer's
 */
class AnnotaterJavadocInlayRenderer(
    private val editor: Editor,
    private val initialOffset: Int,
    private val javadoc: String,
    private val highlights: List<AnnotaterJavadocRendering.LineHighlight>,
    private val pointer: SmartPsiElementPointer<PsiMember>,
) : EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val indent = maxOf(0, startX(inlay) - editor.insets.left)
        return indent + lines().maxOf { line -> line.sumOf { it.width() } }
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return lines().size * editor.lineHeight
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val startX = startX(inlay)
        var y = targetRegion.y + editor.ascent
        for (line in lines()) {
            var x = startX
            for (token in line) {
                g.font = token.font()
                g.color = token.attributes?.foregroundColor ?: editor.colorsScheme.defaultForeground
                g.drawString(token.text, x, y)
                x += token.width()
            }
            y += editor.lineHeight
        }
    }

    override fun getContextMenuGroup(inlay: Inlay<*>): ActionGroup =
        DefaultActionGroup(AnnotaterJavadocInlayPassFactory.EditAction(pointer))

    private fun startX(inlay: Inlay<*>): Int {
        // The size is first calculated while the inlay is being created, before it has a valid offset. The offset
        // differs from initialOffset after the document changes, until the highlighting pass replaces the inlay
        val offset = (if (inlay.isValid) inlay.offset else initialOffset).coerceIn(0, editor.document.textLength)
        val document = editor.document
        val lineStart = document.getLineStartOffset(document.getLineNumber(offset))
        val contentStart = CharArrayUtil.shiftForward(document.immutableCharSequence, lineStart, " \t")
        return editor.offsetToXY(contentStart, false, true).x
    }

    private inner class Token(val text: String, val attributes: TextAttributes?) {
        fun font() = editor.colorsScheme.getFont(EditorFontType.forJavaStyle(attributes?.fontType ?: 0))

        fun width() = editor.contentComponent.getFontMetrics(font()).stringWidth(text)
    }

    private var lines: List<List<Token>>? = null
    private var linesScheme: EditorColorsScheme? = null

    private fun lines(): List<List<Token>> {
        val scheme = editor.colorsScheme
        lines?.takeIf { linesScheme === scheme }?.let { return it }

        val textLines = javadoc.lines().map { if (it.isEmpty()) "///" else "/// $it" }
        val text = textLines.joinToString("\n")
        val attributes = arrayOfNulls<TextAttributes>(text.length)
        val highlighter = LexerEditorHighlighter(JavaFileHighlighter(LanguageLevel.HIGHEST), scheme)
        highlighter.setText(text)
        val iterator = highlighter.createIterator(0)
        while (!iterator.atEnd()) {
            attributes.fill(iterator.textAttributes, iterator.start, iterator.end)
            iterator.advance()
        }

        val lineStarts = textLines.runningFold(0) { start, line -> start + line.length + 1 }
        for (highlight in highlights) {
            val line = textLines.getOrNull(highlight.line) ?: continue
            if (highlight.endColumn > line.length) {
                continue
            }
            val lineStart = lineStarts[highlight.line]
            for (i in lineStart + highlight.startColumn until lineStart + highlight.endColumn) {
                attributes[i] = TextAttributes.merge(attributes[i], highlight.attributes)
            }
        }

        // Splits each line into runs of the same attributes
        val lines = textLines.mapIndexed { index, line ->
            val offset = lineStarts[index]
            val tokens = mutableListOf<Token>()
            var start = 0
            for (end in 1..line.length) {
                if (end == line.length || attributes[offset + end] != attributes[offset + start]) {
                    tokens += Token(line.substring(start, end), attributes[offset + start])
                    start = end
                }
            }
            tokens
        }
        this.lines = lines
        linesScheme = scheme
        return lines
    }
}
