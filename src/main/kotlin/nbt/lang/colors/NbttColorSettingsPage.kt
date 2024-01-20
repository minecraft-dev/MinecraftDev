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

package com.demonwav.mcdev.nbt.lang.colors

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.BYTE
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.DOUBLE
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.FLOAT
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.INT
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.KEYWORD
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.LONG
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.MATERIAL
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.SHORT
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.STRING
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.STRING_NAME
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.UNQUOTED_STRING
import com.demonwav.mcdev.nbt.lang.colors.NbttSyntaxHighlighter.Companion.UNQUOTED_STRING_NAME
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class NbttColorSettingsPage : ColorSettingsPage {

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getHighlighter() = NbttSyntaxHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap() = map
    override fun getAttributeDescriptors() = DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = MCDevBundle("nbt.lang.display_name")
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
                    <uname>nested</uname>: {
                        <uname>list</uname>: [
                            [ 0B, 1B, false, true, 14B ],
                            [
                                {
                                    <uname>nested again</uname>: 0D
                                }
                            ]
                        ]
                    }
                },
            ]
            <uname>materialString</name>: "minecraft<material>:block</material>"
            <name>"stringTest"</name>: "HELLO WORLD THIS IS A TEST STRING ÅÄÖ!"
        }
        """.trimIndent()

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.keyword.display_name"), KEYWORD),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.string.display_name"), STRING),
            AttributesDescriptor(
                MCDevBundle.pointer("nbt.lang.highlighting.unquoted_string.display_name"),
                UNQUOTED_STRING
            ),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.name.display_name"), STRING_NAME),
            AttributesDescriptor(
                MCDevBundle.pointer("nbt.lang.highlighting.unquoted_name.display_name"),
                UNQUOTED_STRING_NAME
            ),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.byte.display_name"), BYTE),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.short.display_name"), SHORT),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.int.display_name"), INT),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.long.display_name"), LONG),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.float.display_name"), FLOAT),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.double.display_name"), DOUBLE),
            AttributesDescriptor(MCDevBundle.pointer("nbt.lang.highlighting.material.display_name"), MATERIAL),
        )

        private val map = mapOf(
            "name" to NbttSyntaxHighlighter.STRING_NAME,
            "uname" to NbttSyntaxHighlighter.UNQUOTED_STRING_NAME,
            "material" to NbttSyntaxHighlighter.MATERIAL,
        )
    }
}
