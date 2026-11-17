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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class UnpickColorSettingsPage : ColorSettingsPage {

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getHighlighter() = UnpickSyntaxHighlighter()
    override fun getDemoText() =
        $$"""
        unpick v4

        #: Colors in ARGB format
        group int <group>ARGBColor</group>
            @format hex
            <class>net.minecraft.util.CommonColors</class>.<field>WHITE</field>
            <class>net.minecraft.util.CommonColors</class>.<field>BLACK</field>

        # apply to floats in the default group globally
        group float
            @strict
            <class>net.minecraft.util.Mth</class>.<field>PI</field> / 3

        group int
            @scope class <class>net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket</class>
            (int) <class>net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket</class>.<field>MAX_PAYLOAD_SIZE</field>:long

        target_method <class>net.minecraft.client.gui.GuiGraphics</class> <method>fill</method> (IIIII)V
            param 4 <group>ARGBColor</group>
        target_field <class>net.minecraft.core.particles.ColorParticleOption</class> <field>color</field> I <group>ARGBColor</group>
        target_annotation <class>net.minecraft.world.level.block.Block$UpdateFlags</class> <group>SetBlockFlag</group>
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap() = Const.TAGS
    override fun getAttributeDescriptors() = Const.DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = MCDevBundle("minecraft.unpick.color_settings.display_name")

    private object Const {
        val TAGS = mapOf(
            "group" to UnpickSyntaxHighlighter.GROUP_NAME,
            "class" to UnpickSyntaxHighlighter.CLASS_NAME,
            "field" to UnpickSyntaxHighlighter.FIELD_NAME,
            "method" to UnpickSyntaxHighlighter.METHOD_NAME,
        )

        val DESCRIPTORS = arrayOf(
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.keyword"),
                UnpickSyntaxHighlighter.KEYWORD,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.attribute"),
                UnpickSyntaxHighlighter.ATTRIBUTE,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.group_name"),
                UnpickSyntaxHighlighter.GROUP_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.class_name"),
                UnpickSyntaxHighlighter.CLASS_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.field_name"),
                UnpickSyntaxHighlighter.FIELD_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.method_name"),
                UnpickSyntaxHighlighter.METHOD_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.descriptor"),
                UnpickSyntaxHighlighter.DESCRIPTOR,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.number"),
                UnpickSyntaxHighlighter.NUMBER,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.string"),
                UnpickSyntaxHighlighter.STRING,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.operator"),
                UnpickSyntaxHighlighter.OPERATOR,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.parentheses"),
                UnpickSyntaxHighlighter.PARENTHESES,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.dot"),
                UnpickSyntaxHighlighter.DOT,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.comment"),
                UnpickSyntaxHighlighter.COMMENT,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.doc_comment"),
                UnpickSyntaxHighlighter.DOC_COMMENT,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.unsupported"),
                UnpickSyntaxHighlighter.UNSUPPORTED,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.unpick.color_settings.bad_character"),
                UnpickSyntaxHighlighter.BAD_CHARACTER,
            ),
        )
    }
}
