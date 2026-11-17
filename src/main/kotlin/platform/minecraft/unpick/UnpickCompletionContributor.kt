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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findMethodsByInternalName
import com.demonwav.mcdev.util.findQualifiedClass
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElementBuilder

/**
 * Completes keywords and descriptors. References (class, field, method and group names) complete themselves.
 *
 * The context is determined from the text of the current line rather than the PSI, which is unreliable in the
 * incomplete code being completed.
 */
class UnpickCompletionContributor : CompletionContributor() {

    override fun beforeCompletion(context: CompletionInitializationContext) {
        // The default dummy identifier ends in a space, which would change how the line is lexed
        context.dummyIdentifier = CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }
        val file = parameters.originalFile as? UnpickFile ?: return
        val text = file.text
        val offset = parameters.offset.coerceAtMost(text.length)
        val lineStart = text.lastIndexOf('\n', offset - 1) + 1
        val line = text.substring(lineStart, offset)
        if ('#' in line) {
            return
        }

        if (lineStart == 0) {
            if (file.header == null && HEADER_PREFIX.matches(line)) {
                result.addElement(keyword("unpick v4"))
            }
            return
        }

        val wordResult = result.withPrefixMatcher(line.takeLastWhile { it.isLetterOrDigit() || it == '_' || it == '$' })

        when {
            ITEM_KEYWORD.matches(line) -> ITEM_KEYWORDS.forEach { wordResult.addElement(keyword(it)) }
            GROUP_DATA_TYPE.matches(line) -> DataType.entries.filter { it.isValidGroupType }
                .forEach { wordResult.addElement(keyword(it.keyword)) }
            ATTRIBUTE.matches(line) -> ATTRIBUTES.forEach { wordResult.addElement(keyword(it)) }
            SCOPE_KIND.matches(line) -> SCOPE_KINDS.forEach { wordResult.addElement(keyword(it)) }
            FORMAT.matches(line) -> GroupFormat.entries.forEach { wordResult.addElement(keyword(it.keyword)) }
            FIELD_SUFFIX.containsMatchIn(line) -> {
                wordResult.addElement(keyword("instance"))
                DataType.entries.forEach { wordResult.addElement(keyword(it.keyword)) }
            }

            CAST.containsMatchIn(line) && currentItemKeyword(text, lineStart) == "group" ->
                DataType.entries.filter { it.isPrimitive }.forEach { wordResult.addElement(keyword(it.keyword)) }

            INDENTED_WORD.matches(line) && currentItemKeyword(text, lineStart) == "target_method" ->
                TARGET_METHOD_ITEMS.forEach { wordResult.addElement(keyword(it)) }
            else -> completeDescriptor(file, line, result)
        }
    }

    private fun completeDescriptor(file: UnpickFile, line: String, result: CompletionResultSet) {
        val methodMatch = TARGET_METHOD_DESCRIPTOR.matchEntire(line) ?: METHOD_SCOPE_DESCRIPTOR.matchEntire(line)
        if (methodMatch != null) {
            val (className, methodName, typed) = methodMatch.destructured
            val psiClass = findQualifiedClass(className.replace(Regex("\\s"), ""), file) ?: return
            val descriptorResult = result.withPrefixMatcher(typed)
            for (method in psiClass.findMethodsByInternalName(methodName.replace(Regex("\\s"), ""))) {
                method.descriptor?.let { descriptorResult.addElement(LookupElementBuilder.create(it)) }
            }
            return
        }

        val fieldMatch = TARGET_FIELD_DESCRIPTOR.matchEntire(line) ?: return
        val (className, fieldName, typed) = fieldMatch.destructured
        val psiClass = findQualifiedClass(className.replace(Regex("\\s"), ""), file) ?: return
        val field = psiClass.findFieldByName(fieldName, true) ?: return
        field.descriptor?.let { result.withPrefixMatcher(typed).addElement(LookupElementBuilder.create(it)) }
    }

    /**
     * Finds the keyword of the item the line starting at [lineStart] belongs to.
     */
    private fun currentItemKeyword(text: String, lineStart: Int): String? {
        var start = lineStart
        while (start > 0) {
            start = text.lastIndexOf('\n', start - 2) + 1
            val end = text.indexOf('\n', start).let { if (it == -1) text.length else it }
            val line = text.substring(start, end)
            if (line.isBlank() || line.trimStart().startsWith('#')) {
                continue
            }
            if (!line[0].isWhitespace()) {
                return line.takeWhile { it.isLetterOrDigit() || it == '_' }
            }
        }
        return null
    }

    private fun keyword(text: String) = LookupElementBuilder.create(text).bold().withIcon(PlatformAssets.MINECRAFT_ICON)

    private companion object {
        val ITEM_KEYWORDS = listOf("group", "target_method", "target_field", "target_annotation")
        val ATTRIBUTES = listOf("scope", "strict", "flags", "format")
        val SCOPE_KINDS = listOf("package", "class", "method")
        val TARGET_METHOD_ITEMS = listOf("param", "return")

        val HEADER_PREFIX = Regex("[a-z]*")
        val ITEM_KEYWORD = Regex("[a-z_]*")
        val GROUP_DATA_TYPE = Regex("group\\s+\\w*")
        val ATTRIBUTE = Regex("\\s+@\\s*\\w*")
        val SCOPE_KIND = Regex("\\s+@\\s*scope\\s+\\w*")
        val FORMAT = Regex("\\s+@\\s*format\\s+\\w*")
        val FIELD_SUFFIX = Regex("\\.[\\w$]+\\s*:\\s*\\w*$")
        val CAST = Regex("\\(\\s*\\w*$")
        val INDENTED_WORD = Regex("\\s+\\w*")

        private const val CLASS_NAME = "([\\w$]+(?:\\s*\\.\\s*[\\w$]+)*)"
        private const val METHOD_NAME = "([\\w$]+|<\\s*\\w+\\s*>)"
        val TARGET_METHOD_DESCRIPTOR = Regex("target_method\\s+$CLASS_NAME\\s+$METHOD_NAME\\s+(\\S*)")
        val METHOD_SCOPE_DESCRIPTOR = Regex("\\s+@\\s*scope\\s+method\\s+$CLASS_NAME\\s+$METHOD_NAME\\s+(\\S*)")
        val TARGET_FIELD_DESCRIPTOR = Regex("target_field\\s+$CLASS_NAME\\s+([\\w$]+)\\s+(\\S*)")
    }
}
