package com.demonwav.mcdev.util;

import com.demonwav.mcdev.resource.MinecraftProjectsIcons;

import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;

public class MinecraftFileTemplateGroupFactory implements FileTemplateGroupDescriptorFactory {
    
    public static final String BUKKIT_MAIN_CLASS_TEMPLATE = "bukkit_main_class.java";
    public static final String BUKKIT_PLUGIN_YML_TEMPLATE = "plugin_template.yml";
    public static final String BUKKIT_POM_TEMPLATE = "bukkit_pom_template.xml";

    public static final String SPONGE_MAIN_CLASS_TEMPLATE = "sponge_main_class.java";
    public static final String SPONGE_POM_TEMPLATE = "sponge_pom_template.xml";

    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Minecraft", MinecraftProjectsIcons.Bukkit);

        group.addTemplate(new FileTemplateGroupDescriptor(BUKKIT_MAIN_CLASS_TEMPLATE, MinecraftProjectsIcons.Bukkit));
        group.addTemplate(new FileTemplateGroupDescriptor(BUKKIT_PLUGIN_YML_TEMPLATE, MinecraftProjectsIcons.Bukkit));
        group.addTemplate(new FileTemplateGroupDescriptor(BUKKIT_POM_TEMPLATE, MinecraftProjectsIcons.Bukkit));

        group.addTemplate(new FileTemplateGroupDescriptor(SPONGE_MAIN_CLASS_TEMPLATE, MinecraftProjectsIcons.Sponge));
        group.addTemplate(new FileTemplateGroupDescriptor(SPONGE_POM_TEMPLATE, MinecraftProjectsIcons.Spigot));

        return group;
    }
}
