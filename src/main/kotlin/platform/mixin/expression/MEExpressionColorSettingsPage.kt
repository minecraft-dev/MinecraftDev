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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class MEExpressionColorSettingsPage : ColorSettingsPage {
    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.string.display_name"),
                MEExpressionSyntaxHighlighter.STRING
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.string_escape.display_name"),
                MEExpressionSyntaxHighlighter.STRING_ESCAPE
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.number.display_name"),
                MEExpressionSyntaxHighlighter.NUMBER
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.keyword.display_name"),
                MEExpressionSyntaxHighlighter.KEYWORD
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.operator.display_name"),
                MEExpressionSyntaxHighlighter.OPERATOR
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.parens.display_name"),
                MEExpressionSyntaxHighlighter.PARENS
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.brackets.display_name"),
                MEExpressionSyntaxHighlighter.BRACKETS
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.braces.display_name"),
                MEExpressionSyntaxHighlighter.BRACES
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.dot.display_name"),
                MEExpressionSyntaxHighlighter.DOT
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.comma.display_name"),
                MEExpressionSyntaxHighlighter.COMMA
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.capture.display_name"),
                MEExpressionSyntaxHighlighter.CAPTURE
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.wildcard.display_name"),
                MEExpressionSyntaxHighlighter.WILDCARD
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.identifier.display_name"),
                MEExpressionSyntaxHighlighter.IDENTIFIER
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.call_identifier.display_name"),
                MEExpressionSyntaxHighlighter.IDENTIFIER_CALL
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.class_name_identifier.display_name"),
                MEExpressionSyntaxHighlighter.IDENTIFIER_CLASS_NAME
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.primitive_type_identifier.display_name"),
                MEExpressionSyntaxHighlighter.IDENTIFIER_PRIMITIVE_TYPE
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.member_name_identifier.display_name"),
                MEExpressionSyntaxHighlighter.IDENTIFIER_MEMBER_NAME
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.variable_identifier.display_name"),
                MEExpressionSyntaxHighlighter.IDENTIFIER_VARIABLE
            ),
            AttributesDescriptor(
                MCDevBundle.pointer(
                    "mixinextras.expression.lang.highlighting.type_declaration_identifier.display_name"
                ),
                MEExpressionSyntaxHighlighter.IDENTIFIER_TYPE_DECLARATION
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.declaration_identifier.display_name"),
                MEExpressionSyntaxHighlighter.IDENTIFIER_DECLARATION
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("mixinextras.expression.lang.highlighting.bad_char.display_name"),
                MEExpressionSyntaxHighlighter.BAD_CHAR
            ),
        )

        private val TAGS = mapOf(
            "call" to MEExpressionSyntaxHighlighter.IDENTIFIER_CALL,
            "class_name" to MEExpressionSyntaxHighlighter.IDENTIFIER_CLASS_NAME,
            "member_name" to MEExpressionSyntaxHighlighter.IDENTIFIER_MEMBER_NAME,
            "primitive_type" to MEExpressionSyntaxHighlighter.IDENTIFIER_PRIMITIVE_TYPE,
            "variable" to MEExpressionSyntaxHighlighter.IDENTIFIER_VARIABLE,
        )
    }

    override fun getIcon() = PlatformAssets.MIXIN_ICON
    override fun getHighlighter() = MEExpressionSyntaxHighlighter()

    override fun getDemoText() = """
        <variable>variable</variable>.<call>function</call>(
            'a string with \\ escapes',
            123 + @(45),
            ?,
            <class_name>ClassName</class_name>.class,
            <variable>foo</variable>.<member_name>bar</member_name>,
            new <primitive_type>int</primitive_type>[] { 1, 2, 3 },
            'a bad character: ' # other_identifier
        )[0]
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap() = TAGS
    override fun getAttributeDescriptors() = DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = MCDevBundle("mixinextras.expression.lang.display_name")
}
