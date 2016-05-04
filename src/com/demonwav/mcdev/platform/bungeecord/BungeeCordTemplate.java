package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;
import com.demonwav.mcdev.util.AbstractTemplate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.Properties;

public class BungeeCordTemplate extends AbstractTemplate {

    public static void applyMainClassTemplate(Project project, VirtualFile file, String packageName, String className) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUNGEECORD_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyPluginDescriptionFileTemplate(Project project, VirtualFile file, BungeeCordSettings settings) {
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
        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
