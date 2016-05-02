package com.demonwav.mcdev.util;

import com.demonwav.mcdev.Type;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.IOException;
import java.util.Properties;

public class MinecraftTemplate {

    public static String applyPomTemplate(Project project, String version) {
        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", version);

        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate fileTemplate = manager.getTemplate(MinecraftFileTemplateGroupFactory.BUKKIT_POM_TEMPLATE);
        try {
            return fileTemplate.getText(properties);
        } catch (IOException e) {
            // TODO what to do when this fails?
            e.printStackTrace();
            return "";
        }
    }

    public static void applyMainBukkitClassTemplate(Project project, VirtualFile file, String packageName, String className, boolean bukkit) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);
        if (bukkit) {
            properties.setProperty("BUKKIT", "true");
        }

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyMainSpongeClassTemplate(Project project, VirtualFile file, String packageName, String className) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.SPONGE_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyPluginYmlTemplate(Project project, VirtualFile file, Type type, BukkitSettings settings, String groupId) {
        Properties properties = new Properties();

        properties.setProperty("NAME", settings.pluginName);
        properties.setProperty("VERSION", settings.pluginVersion);
        properties.setProperty("DESCRIPTION", settings.description);
        properties.setProperty("MAIN", groupId + "." + settings.mainClass);
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

        // These are bukkit and spigot only settings
        if (type != Type.BUNGEECORD) {
            properties.setProperty("WEBSITE", settings.website);
            properties.setProperty("DATABASE", Boolean.toString(settings.database));
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
            if (settings.hasDatabase()) {
                properties.setProperty("HAS_DATABASE", "true");
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
        }
        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void applyTemplate(Project project, VirtualFile file, String template, Properties properties) throws IOException {
        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate fileTemplate = manager.getTemplate(template);

        // remove comments (they leave empty lines
        fileTemplate.setText(fileTemplate.getText().replaceAll("#\\*.*\\*#\\n", "").trim());

        Properties allProperties = manager.getDefaultProperties();
        allProperties.putAll(properties);

        VfsUtil.saveText(file, fileTemplate.getText(allProperties));

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            new ReformatCodeProcessor(project, psiFile, null, false).run();
        }
    }
}
