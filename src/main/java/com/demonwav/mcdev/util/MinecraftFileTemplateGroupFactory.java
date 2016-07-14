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

    public static final String SPONGE_BUILD_GRADLE_TEMPLATE = "sponge_build.gradle";
    public static final String SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE = "sponge_submodule_build.gradle";
    public static final String SPONGE_MAIN_CLASS_TEMPLATE = "sponge_main_class.java";
    public static final String SPONGE_POM_TEMPLATE = "sponge_pom_template.xml";

    public static final String FORGE_BUILD_GRADLE_TEMPLATE = "forge_build.gradle";
    public static final String FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE = "forge_submodule_build.gradle";
    public static final String FORGE_MAIN_CLASS_TEMPLATE = "forge_main_class.java";
    public static final String MCMOD_INFO_TEMPLATE = "mcmod.info";

    public static final String MULTI_MODULE_BUILD_GRADLE_TEMPLATE = "multi_module_build.gradle";
    public static final String BUILD_GRADLE_TEMPLATE = "build.gradle";
    public static final String SETTINGS_GRADLE_TEMPLATE = "settings.gradle";
    public static final String SUBMODULE_BUILD_GRADLE_TEMPLATE = "submodule_build.gradle";

    public static final String LITELOADER_BUILD_GRADLE_TEMPLATE = "liteloader_build.gradle";
    public static final String LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE = "liteloader_submodule_build.gradle";
    public static final String LITELOADER_MAIN_CLASS_TEMPLATE = "liteloader_main_class.java";

    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Minecraft", PlatformAssets.MINECRAFT_ICON);

        group.addTemplate(new FileTemplateDescriptor(BUKKIT_MAIN_CLASS_TEMPLATE, PlatformAssets.BUKKIT_ICON));
        group.addTemplate(new FileTemplateDescriptor(BUKKIT_PLUGIN_YML_TEMPLATE, PlatformAssets.BUKKIT_ICON));
        group.addTemplate(new FileTemplateDescriptor(BUKKIT_POM_TEMPLATE, PlatformAssets.BUKKIT_ICON));

        group.addTemplate(new FileTemplateDescriptor(BUNGEECORD_MAIN_CLASS_TEMPLATE, PlatformAssets.BUNGEECORD_ICON));
        group.addTemplate(new FileTemplateDescriptor(BUNGEECORD_PLUGIN_YML_TEMPLATE, PlatformAssets.BUNGEECORD_ICON));
        group.addTemplate(new FileTemplateDescriptor(BUNGEECORD_POM_TEMPLATE, PlatformAssets.BUNGEECORD_ICON));

        group.addTemplate(new FileTemplateDescriptor(SPONGE_BUILD_GRADLE_TEMPLATE, PlatformAssets.SPONGE_ICON));
        group.addTemplate(new FileTemplateDescriptor(SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.SPONGE_ICON));
        group.addTemplate(new FileTemplateDescriptor(SPONGE_MAIN_CLASS_TEMPLATE, PlatformAssets.SPONGE_ICON));
        group.addTemplate(new FileTemplateDescriptor(SPONGE_POM_TEMPLATE, PlatformAssets.SPONGE_ICON));

        group.addTemplate(new FileTemplateDescriptor(FORGE_BUILD_GRADLE_TEMPLATE, PlatformAssets.FORGE_ICON));
        group.addTemplate(new FileTemplateDescriptor(FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.FORGE_ICON));
        group.addTemplate(new FileTemplateDescriptor(FORGE_MAIN_CLASS_TEMPLATE, PlatformAssets.FORGE_ICON));
        group.addTemplate(new FileTemplateDescriptor(MCMOD_INFO_TEMPLATE, PlatformAssets.FORGE_ICON));

        group.addTemplate(new FileTemplateDescriptor(BUILD_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON));
        group.addTemplate(new FileTemplateDescriptor(MULTI_MODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON));
        group.addTemplate(new FileTemplateDescriptor(SETTINGS_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON));
        group.addTemplate(new FileTemplateDescriptor(SUBMODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON));

        group.addTemplate(new FileTemplateDescriptor(LITELOADER_BUILD_GRADLE_TEMPLATE, PlatformAssets.LITELOADER_ICON));
        group.addTemplate(new FileTemplateDescriptor(LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.LITELOADER_ICON));
        group.addTemplate(new FileTemplateDescriptor(LITELOADER_MAIN_CLASS_TEMPLATE, PlatformAssets.LITELOADER_ICON));

        return group;
    }
}
