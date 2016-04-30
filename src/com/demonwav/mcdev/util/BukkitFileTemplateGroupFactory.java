/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.util;

import com.demonwav.mcdev.icons.BukkitProjectsIcons;

import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;

public class BukkitFileTemplateGroupFactory implements FileTemplateGroupDescriptorFactory {
    
    public static final String BUKKIT_MAIN_CLASS_TEMPLATE = "main_class.java";
    public static final String BUKKIT_PLUGIN_YML_TEMPLATE = "plugin_template.yml";
    public static final String BUKKIT_POM_TEMPLATE = "pom_template.xml";

    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Bukkit", BukkitProjectsIcons.BukkitProject);

        group.addTemplate(new FileTemplateGroupDescriptor(BUKKIT_MAIN_CLASS_TEMPLATE, BukkitProjectsIcons.BukkitProject));
        group.addTemplate(new FileTemplateGroupDescriptor(BUKKIT_PLUGIN_YML_TEMPLATE, BukkitProjectsIcons.BukkitProject));
        group.addTemplate(new FileTemplateGroupDescriptor(BUKKIT_POM_TEMPLATE, BukkitProjectsIcons.BukkitProject));

        return group;
    }
}
