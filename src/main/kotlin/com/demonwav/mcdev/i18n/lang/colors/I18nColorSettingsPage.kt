/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.colors

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.lang.I18nLexerAdapter
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class I18nColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getHighlighter() = I18nSyntaxHighlighter(I18nLexerAdapter())
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
            AttributesDescriptor("Key", I18nSyntaxHighlighter.KEY),
            AttributesDescriptor("Separator", I18nSyntaxHighlighter.EQUALS),
            AttributesDescriptor("Value", I18nSyntaxHighlighter.VALUE),
            AttributesDescriptor("Comments", I18nSyntaxHighlighter.COMMENT)
        )
    }
}
