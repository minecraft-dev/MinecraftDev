/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
            AttributesDescriptor("Comments", LangSyntaxHighlighter.COMMENT)
        )
    }
}
