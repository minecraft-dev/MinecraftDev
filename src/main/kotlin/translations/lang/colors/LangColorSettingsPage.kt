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

package com.demonwav.mcdev.translations.lang.colors

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.translations.lang.LangLexerAdapter
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class LangColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getHighlighter() = LangSyntaxHighlighter(LangLexerAdapter())
    override fun getAdditionalHighlightingTagToDescriptorMap() = emptyMap<String, TextAttributesKey>()
    override fun getAttributeDescriptors() = DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = "Minecraft localization"
    override fun getDemoText() =
        """
        # This is a comment
        path.to.key=This is a value
        """.trimIndent()

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Key", LangSyntaxHighlighter.KEY),
            AttributesDescriptor("Separator", LangSyntaxHighlighter.EQUALS),
            AttributesDescriptor("Value", LangSyntaxHighlighter.VALUE),
            AttributesDescriptor("Comments", LangSyntaxHighlighter.COMMENT),
        )
    }
}
