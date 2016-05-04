package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;
import com.demonwav.mcdev.util.AbstractTemplate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.Properties;

public class BukkitTemplate extends AbstractTemplate {

    public static void applyMainClassTemplate(Project project, VirtualFile file, String packageName, String className) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyPluginDescriptionFileTemplate(Project project, VirtualFile file, BukkitSettings settings) {
        Properties properties = new Properties();

        properties.setProperty("NAME", settings.pluginName);
        properties.setProperty("VERSION", settings.pluginVersion);
        properties.setProperty("DESCRIPTION", settings.description);
        properties.setProperty("MAIN", settings.mainClass);
        properties.setProperty("AUTHOR", settings.author);
        properties.setProperty("DEPEND", settings.depend.toString());
        properties.setProperty("SOFT_DEPEND", settings.softDepend.toString());

        if (settings.hasDescription()) {
            properties.setProperty("HAS_DESCRIPTION", "true");
        }
        if (settings.hasAuthor()) {
            properties.setProperty("HAS_AUTHOR", "true");
        }
        if (settings.hasDepend()) {
            properties.setProperty("HAS_DEPEND", "true");
        }
        if (settings.hasSoftDepend()) {
            properties.setProperty("HAS_SOFT_DEPEND", "true");
        }

        properties.setProperty("WEBSITE", settings.website);
        properties.setProperty("PREFIX", settings.prefix);
        properties.setProperty("LOAD", settings.load.name());
        properties.setProperty("AUTHOR_LIST", settings.authorList.toString());
        properties.setProperty("LOAD_BEFORE", settings.loadBefore.toString());

        if (settings.hasAuthorList()) {
            properties.setProperty("HAS_AUTHOR_LIST", "true");
        }
        if (settings.hasWebsite()) {
            properties.setProperty("HAS_WEBSITE", "true");
        }
        if (settings.hasLoad()) {
            properties.setProperty("HAS_LOAD", "true");
        }
        if (settings.hasPrefix()) {
            properties.setProperty("HAS_PREFIX", "true");
        }
        if (settings.hasLoadBefore()) {
            properties.setProperty("HAS_LOAD_BEFORE", "true");
        }
        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
