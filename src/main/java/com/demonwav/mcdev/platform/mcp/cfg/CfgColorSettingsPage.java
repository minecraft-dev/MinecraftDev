package com.demonwav.mcdev.platform.mcp.cfg;

import com.demonwav.mcdev.asset.PlatformAssets;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import javax.swing.Icon;

public class CfgColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[] {
        new AttributesDescriptor("Keyword", CfgSyntaxHighlighter.KEYWORD),
        new AttributesDescriptor("Class Name", CfgSyntaxHighlighter.CLASS_NAME),
        new AttributesDescriptor("Class Value", CfgSyntaxHighlighter.CLASS_VALUE),
        new AttributesDescriptor("Primitive Value", CfgSyntaxHighlighter.PRIMITIVE),
        new AttributesDescriptor("Element Name", CfgSyntaxHighlighter.ELEMENT_NAME),
        new AttributesDescriptor("Asterisk", CfgSyntaxHighlighter.ASTERISK),
        new AttributesDescriptor("Comment", CfgSyntaxHighlighter.COMMENT)
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return PlatformAssets.FORGE_ICON;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new CfgSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "# Minecraft\n" +
            "public net.minecraft.block.BlockFlowerPot func_149928_a(Lnet/minecraft/block/Block;I)Z # canNotContain\n" +
            "private-f net.minecraft.inventory.ContainerChest field_7515F4_f # numRows\n" +
            "protected net.minecraft.block.state.BlockStateContainer$StateImplementation\n" +
            "public-f net.minecraft.item.Item func_77656_e(I)Lnet/minecraft/item/Item; # setMaxDamage\n" +
            "public-f net.minecraft.server.management.UserList *\n" +
            "public net.minecraft.world.demo.DemoWorldServer func_175680_a(IIZ)Z # isChunkLoaded";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "CFG";
    }
}
