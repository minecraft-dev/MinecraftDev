/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class AtColorSettingsPage : ColorSettingsPage {

    override fun getIcon() = PlatformAssets.MCP_ICON
    override fun getHighlighter() = AtSyntaxHighlighter()
    override fun getDemoText() =
        """
        # Minecraft

        public net.minecraft.block.BlockFlowerPot func_149928_a(Lnet/minecraft/block/Block;I)Z # canNotContain
        private-f net.minecraft.inventory.ContainerChest field_7515F4_f # numRows
        protected net.minecraft.block.state.BlockStateContainer${'$'}StateImplementation
        public-f net.minecraft.item.Item func_77656_e(I)Lnet/minecraft/item/Item; # setMaxDamage
        public+f net.minecraft.server.management.UserList *
        default+f net.minecraft.item.Item *()
        public net.minecraft.world.demo.DemoWorldServer func_175680_a(IIZ)Z # isChunkLoaded
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors() = DESCRIPTORS
    override fun getColorDescriptors(): Array<out ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = "Access Transformers"

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", AtSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("Class Name", AtSyntaxHighlighter.CLASS_NAME),
            AttributesDescriptor("Class Value", AtSyntaxHighlighter.CLASS_VALUE),
            AttributesDescriptor("Primitive Value", AtSyntaxHighlighter.PRIMITIVE),
            AttributesDescriptor("Element Name", AtSyntaxHighlighter.ELEMENT_NAME),
            AttributesDescriptor("Asterisk", AtSyntaxHighlighter.ASTERISK),
            AttributesDescriptor("Comment", AtSyntaxHighlighter.COMMENT)
        )
    }
}
