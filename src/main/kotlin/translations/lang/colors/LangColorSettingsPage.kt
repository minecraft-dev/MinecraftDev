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

package com.demonwav.mcdev.translations.lang.colors

import com.demonwav.mcdev.asset.MCDevBundle
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
    override fun getAttributeDescriptors() = Const.DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = MCDevBundle("translations.color_settings.display_name")
    override fun getDemoText() =
        """
        # This is a comment
        path.to.key=This is a value
        """.trimIndent()

    private object Const {
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor(MCDevBundle.pointer("translations.color_settings.key"), LangSyntaxHighlighter.KEY),
            AttributesDescriptor(
                MCDevBundle.pointer("translations.color_settings.separator"),
                LangSyntaxHighlighter.EQUALS,
            ),
            AttributesDescriptor(MCDevBundle.pointer("translations.color_settings.value"), LangSyntaxHighlighter.VALUE),
            AttributesDescriptor(
                MCDevBundle.pointer("translations.color_settings.comments"),
                LangSyntaxHighlighter.COMMENT,
            ),
        )
    }
}
