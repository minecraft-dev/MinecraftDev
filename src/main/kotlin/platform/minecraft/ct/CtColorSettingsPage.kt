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

package com.demonwav.mcdev.platform.minecraft.ct

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class CtColorSettingsPage : ColorSettingsPage {

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getHighlighter() = CtSyntaxHighlighter()
    override fun getDemoText() =
        $$"""
        classTweaker	v2	named
        
        # https://docs.fabricmc.net/develop/class-tweakers/
        
        extendable class net/minecraft/world/item/crafting/Ingredient
        transitive-extendable class net/minecraft/world/item/crafting/Ingredient
        accessible class net/minecraft/world/entity/monster/Phantom$AttackPhase
        transitive-accessible class net/minecraft/world/entity/monster/Phantom$AttackPhase
        extendable method net/minecraft/server/players/IpBanList getIpFromAddress (Ljava/net/SocketAddress;)Ljava/lang/String;
        extendable method net/minecraft/world/item/crafting/Ingredient <init> (Ljava/util/stream/Stream;)V
        accessible field net/minecraft/world/item/crafting/Ingredient values [Lnet/minecraft/world/item/crafting/Ingredient$Value;
        inject-interface net/minecraft/world/level/block/Block com/example/MyBlockInterface
        transitive-inject-interface net/minecraft/world/level/block/Block com/example/MyTransitiveBlockInterface<TT;>
        extend-enum net/minecraft/client/gui/Gui$HeartType MYMOD_HEART_TYPE
        transitive-extend-enum net/minecraft/client/gui/Gui$HeartType MYMOD_TRANSITIVE_HEART_TYPE
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors() = Const.DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = MCDevBundle("minecraft.ct.color_settings.display_name")

    private object Const {
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.header_name"),
                CtSyntaxHighlighter.HEADER_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.header_namespace"),
                CtSyntaxHighlighter.HEADER_NAMESPACE,
            ),
            AttributesDescriptor(MCDevBundle.pointer("minecraft.ct.color_settings.access"), CtSyntaxHighlighter.ACCESS),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.inject_interface"),
                CtSyntaxHighlighter.INJECT_INTERFACE,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.extend_enum"),
                CtSyntaxHighlighter.EXTEND_ENUM,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.class_element"),
                CtSyntaxHighlighter.CLASS_ELEMENT,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.method_element"),
                CtSyntaxHighlighter.METHOD_ELEMENT,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.field_element"),
                CtSyntaxHighlighter.FIELD_ELEMENT,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.class_name"),
                CtSyntaxHighlighter.CLASS_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.member_name"),
                CtSyntaxHighlighter.MEMBER_NAME,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.class_value"),
                CtSyntaxHighlighter.CLASS_VALUE,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.primitive"),
                CtSyntaxHighlighter.PRIMITIVE,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.type_variable"),
                CtSyntaxHighlighter.TYPE_VARIABLE,
            ),
            AttributesDescriptor(
                MCDevBundle.pointer("minecraft.ct.color_settings.comment"),
                CtSyntaxHighlighter.COMMENT,
            ),
        )
    }
}
