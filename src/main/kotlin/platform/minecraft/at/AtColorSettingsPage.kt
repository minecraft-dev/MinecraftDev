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

package com.demonwav.mcdev.platform.minecraft.at

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class AtColorSettingsPage : ColorSettingsPage {

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getHighlighter() = AtSyntaxHighlighter()
    override fun getDemoText() =
        $$"""
        # Minecraft

        public net.minecraft.block.BlockFlowerPot func_149928_a(Lnet/minecraft/block/Block;I)Z # canNotContain
        private-f net.minecraft.inventory.ContainerChest field_7515F4_f # numRows
        protected net.minecraft.block.state.BlockStateContainer$StateImplementation
        public-f net.minecraft.item.Item func_77656_e(I)Lnet/minecraft/item/Item; # setMaxDamage
        public+f net.minecraft.server.management.UserList *
        default+f net.minecraft.item.Item *()
        public net.minecraft.world.demo.DemoWorldServer func_175680_a(IIZ)Z # isChunkLoaded
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors() = Constants.DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = MCDevBundle("minecraft.at.color_settings.display_name")

    private object Constants {
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.at.color_settings.keyword"),
                AtSyntaxHighlighter.KEYWORD,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.at.color_settings.class_name"),
                AtSyntaxHighlighter.CLASS_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.at.color_settings.class_value"),
                AtSyntaxHighlighter.CLASS_VALUE,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.at.color_settings.primitive_value"),
                AtSyntaxHighlighter.PRIMITIVE,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.at.color_settings.element_name"),
                AtSyntaxHighlighter.ELEMENT_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.at.color_settings.asterisk"),
                AtSyntaxHighlighter.ASTERISK,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.at.color_settings.comment"),
                AtSyntaxHighlighter.COMMENT,
            ),
        )
    }
}
