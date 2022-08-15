/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class AwColorSettingsPage : ColorSettingsPage {

    override fun getIcon() = PlatformAssets.MCP_ICON
    override fun getHighlighter() = AwSyntaxHighlighter()
    override fun getDemoText() =
        """
        accessWidener	v1	named
        
        # https://www.fabricmc.net/wiki/tutorial:accesswideners
        
        extendable class net/minecraft/world/item/crafting/Ingredient
        transitive-extendable class net/minecraft/world/item/crafting/Ingredient
        accessible class net/minecraft/world/entity/monster/Phantom${'$'}AttackPhase
        transitive-accessible class net/minecraft/world/entity/monster/Phantom${'$'}AttackPhase
        extendable method net/minecraft/server/players/IpBanList getIpFromAddress (Ljava/net/SocketAddress;)Ljava/lang/String;
        extendable method net/minecraft/world/item/crafting/Ingredient <init> (Ljava/util/stream/Stream;)V
        accessible field net/minecraft/world/item/crafting/Ingredient values [Lnet/minecraft/world/item/crafting/Ingredient${'$'}Value;
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors() = DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = "Access Wideners"

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Header Name", AwSyntaxHighlighter.HEADER_NAME),
            AttributesDescriptor("Header Namespace", AwSyntaxHighlighter.HEADER_NAMESPACE),
            AttributesDescriptor("Access", AwSyntaxHighlighter.ACCESS),
            AttributesDescriptor("Class Element", AwSyntaxHighlighter.CLASS_ELEMENT),
            AttributesDescriptor("Method Element", AwSyntaxHighlighter.METHOD_ELEMENT),
            AttributesDescriptor("Field Element", AwSyntaxHighlighter.FIELD_ELEMENT),
            AttributesDescriptor("Class Name", AwSyntaxHighlighter.CLASS_NAME),
            AttributesDescriptor("Member Name", AwSyntaxHighlighter.MEMBER_NAME),
            AttributesDescriptor("Class Value", AwSyntaxHighlighter.CLASS_VALUE),
            AttributesDescriptor("Primitive", AwSyntaxHighlighter.PRIMITIVE),
            AttributesDescriptor("Comment", AwSyntaxHighlighter.COMMENT),
        )
    }
}
