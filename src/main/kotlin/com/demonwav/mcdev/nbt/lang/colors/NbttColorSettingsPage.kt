/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.colors

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class NbttColorSettingsPage : ColorSettingsPage {

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getHighlighter() = NbttSyntaxHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap() = map
    override fun getAttributeDescriptors() = DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = "NBT Text"
    override fun getDemoText() =
        """
        <name>"Level"</name>: {
            <name>"byteTest"</name>: 127B
            <name>"shortTest"</name>: 32767S
            <name>"intTest"</name>: 42345
            <uname>longTest</uname>: 9223372036854775807L
            <uname>floatTest</uname>: 0.49823147F
            <uname>key with spaces</uname>: string with whitespaces
            <name>"doubleTest"</name>: 0.4931287132182315
            <name>"byteArray"</name>: bytes(1, 2, 3, 4b, 5B)
            <name>"intArray"</name>: ints(1, 2, 3, 4i, 5I)
            <name>"longArray"</name>: longs(1, 2, 3, 4l, 5L)
            <uname>boolArray</uname>: bytes(true, false, true)
            <name>"listTest (long)"</name>: [ 11L, 12L, 13L, 14L, 15L ]
            <name>"listTest (compound)"</name>: [
                {
                    <name>"created-on"</name>: 1264099775885L
                    <name>"name"</name>: "Compound tag #0"
                },
                {
                    <uname>created-on</uname>: 1264099775885L
                    <uname>name</uname>: "Compound tag #1"
                },
            ]
            <uname>materialString</name>: "minecraft<material>:block</material>"
            <name>"stringTest"</name>: "HELLO WORLD THIS IS A TEST STRING ÅÄÖ!"
        }
        """.trimIndent()

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", NbttSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("String", NbttSyntaxHighlighter.STRING),
            AttributesDescriptor("Unquoted String", NbttSyntaxHighlighter.UNQUOTED_STRING),
            AttributesDescriptor("Name", NbttSyntaxHighlighter.STRING_NAME),
            AttributesDescriptor("Unquoted Name", NbttSyntaxHighlighter.UNQUOTED_STRING_NAME),
            AttributesDescriptor("Byte", NbttSyntaxHighlighter.BYTE),
            AttributesDescriptor("Short", NbttSyntaxHighlighter.SHORT),
            AttributesDescriptor("Int", NbttSyntaxHighlighter.INT),
            AttributesDescriptor("Long", NbttSyntaxHighlighter.LONG),
            AttributesDescriptor("Float", NbttSyntaxHighlighter.FLOAT),
            AttributesDescriptor("Double", NbttSyntaxHighlighter.DOUBLE),
            AttributesDescriptor("Material", NbttSyntaxHighlighter.MATERIAL)
        )

        private val map = mapOf(
            "name" to NbttSyntaxHighlighter.STRING_NAME,
            "uname" to NbttSyntaxHighlighter.UNQUOTED_STRING_NAME,
            "material" to NbttSyntaxHighlighter.MATERIAL
        )
    }
}
