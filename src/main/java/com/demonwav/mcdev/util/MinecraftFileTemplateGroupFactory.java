package com.demonwav.mcdev.util;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;

public class MinecraftFileTemplateGroupFactory implements FileTemplateGroupDescriptorFactory {
    
    public static final String BUKKIT_MAIN_CLASS_TEMPLATE = "bukkit_main_class.java";
    public static final String BUKKIT_PLUGIN_YML_TEMPLATE = "bukkit_plugin_description_file.yml";
    public static final String BUKKIT_POM_TEMPLATE = "bukkit_pom_template.xml";

    public static final String BUNGEECORD_MAIN_CLASS_TEMPLATE = "bungeecord_main_class.java";
    public static final String BUNGEECORD_PLUGIN_YML_TEMPLATE = "bungeecord_plugin_description_file.yml";
    public static final String BUNGEECORD_POM_TEMPLATE = "bungeecord_pom_template.xml";

    public static final String SPONGE_MAIN_CLASS_TEMPLATE = "sponge_main_class.java";
    public static final String SPONGE_POM_TEMPLATE = "sponge_pom_template.xml";

    public static final String BUILD_GRADLE_TEMPLATE = "build.gradle";

    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Minecraft", PlatformAssets.MINECRAFT_ICON);

        group.addTemplate(new FileTemplateDescriptor(BUKKIT_MAIN_CLASS_TEMPLATE, PlatformAssets.BUKKIT_ICON));
        group.addTemplate(new FileTemplateDescriptor(BUKKIT_PLUGIN_YML_TEMPLATE, PlatformAssets.BUKKIT_ICON));
        group.addTemplate(new FileTemplateDescriptor(BUKKIT_POM_TEMPLATE, PlatformAssets.BUKKIT_ICON));

        group.addTemplate(new FileTemplateDescriptor(BUNGEECORD_MAIN_CLASS_TEMPLATE, PlatformAssets.BUNGEECORD_ICON));
        group.addTemplate(new FileTemplateDescriptor(BUNGEECORD_PLUGIN_YML_TEMPLATE, PlatformAssets.BUNGEECORD_ICON));
        group.addTemplate(new FileTemplateDescriptor(BUNGEECORD_POM_TEMPLATE, PlatformAssets.BUNGEECORD_ICON));

        group.addTemplate(new FileTemplateDescriptor(SPONGE_MAIN_CLASS_TEMPLATE, PlatformAssets.SPONGE_ICON));
        group.addTemplate(new FileTemplateDescriptor(SPONGE_POM_TEMPLATE, PlatformAssets.SPONGE_ICON));

        group.addTemplate(new FileTemplateDescriptor(BUILD_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON));

        return group;
    }
}
