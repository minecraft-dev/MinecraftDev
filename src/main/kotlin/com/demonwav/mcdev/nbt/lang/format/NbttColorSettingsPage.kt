/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.format

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.nbt.lang.NbttSyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class NbttColorSettingsPage : ColorSettingsPage {

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getHighlighter() = NbttSyntaxHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap() = null
    override fun getAttributeDescriptors() = DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = "NBT Text"
    override fun getDemoText() =
        """
        "Level": {
            "byteTest": 127B
            "shortTest": 32767S
            "intTest": 42345
            "longTest": 9223372036854775807L
            "floatTest": 0.49823147F
            "doubleTest": 0.4931287132182315
            "byteArray": bytes(1, 2, 3, 4b, 5B)
            "intArray": ints(1, 2, 3, 4i, 5I)
            "listTest (long)": [ 11L, 12L, 13L, 14L, 15L ]
            "listTest (compound)": [
                {
                    "created-on": 1264099775885L
                    "name": "Compound tag #0"
                },
                {
                    "created-on": 1264099775885L
                    "name": "Compound tag #1"
                },
            ]
            "nested compound test": {
                "egg": {
                    "name": "Eggbert"
                    "value": 2000.5F
                }
                "ham": {
                    "name": "Hampus"
                    "value": 0.75F
                }
            }
            "stringTest": "HELLO WORLD THIS IS A TEST STRING ÅÄÖ!"
        }
        """.trimIndent()

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", NbttSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("String", NbttSyntaxHighlighter.STRING),
            AttributesDescriptor("Byte", NbttSyntaxHighlighter.BYTE),
            AttributesDescriptor("Short", NbttSyntaxHighlighter.SHORT),
            AttributesDescriptor("Int", NbttSyntaxHighlighter.INT),
            AttributesDescriptor("Long", NbttSyntaxHighlighter.LONG),
            AttributesDescriptor("Float", NbttSyntaxHighlighter.FLOAT),
            AttributesDescriptor("Double", NbttSyntaxHighlighter.DOUBLE)
        )
    }
}
