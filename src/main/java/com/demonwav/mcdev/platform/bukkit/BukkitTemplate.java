package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder;
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.Properties;

public class BukkitTemplate extends AbstractTemplate {

    public static void applyMainClassTemplate(Module module, VirtualFile file, String packageName, String className) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(module, file, MinecraftFileTemplateGroupFactory.BUKKIT_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String applyPomTemplate(Module module, String version) {
        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", version);

        FileTemplateManager manager = FileTemplateManager.getInstance(module.getProject());
        FileTemplate fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUKKIT_POM_TEMPLATE);
        try {
            return fileTemplate.getText(properties);
        } catch (IOException e) {
            // TODO what to do when this fails?
            e.printStackTrace();
            return "";
        }
    }

    public static void applyPluginDescriptionFileTemplate(Module module, VirtualFile file, BukkitProjectConfiguration settings) {
        Properties properties = new Properties();

        properties.setProperty("NAME", settings.pluginName);
        properties.setProperty("VERSION", settings.pluginVersion);
        properties.setProperty("MAIN", settings.mainClass);

        if (settings.hasPrefix()) {
            properties.setProperty("PREFIX", settings.prefix);
            properties.setProperty("HAS_PREFIX", "true");
        }

        if (settings.loadOrder != LoadOrder.POSTWORLD) {
            properties.setProperty("LOAD", LoadOrder.STARTUP.name());
            properties.setProperty("HAS_LOAD", "true");
        }

        if (settings.hasLoadBefore()) {
            properties.setProperty("LOAD_BEFORE", settings.loadBefore.toString());
            properties.setProperty("HAS_LOAD_BEFORE", "true");
        }

        if (settings.hasDependencies()) {
            properties.setProperty("DEPEND", settings.dependencies.toString());
            properties.setProperty("HAS_DEPEND", "true");
        }

        if (settings.hasSoftDependencies()) {
            properties.setProperty("SOFT_DEPEND", settings.softDependencies.toString());
            properties.setProperty("HAS_SOFT_DEPEND", "true");
        }

        if (settings.hasAuthors()) {
            properties.setProperty("HAS_AUTHOR_LIST", "true");
        }

        if (settings.hasDescription()) {
            properties.setProperty("DESCRIPTION", settings.description);
            properties.setProperty("HAS_DESCRIPTION", "true");
        }

        if (settings.hasWebsite()) {
            properties.setProperty("WEBSITE", settings.website);
            properties.setProperty("HAS_WEBSITE", "true");
        }

        try {
            applyTemplate(module, file, MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE, properties, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
