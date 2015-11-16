/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.util;

import com.demonwav.bukkitplugin.creator.Type;
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

public class BukkitTemplate {

    public static void applyPomTemplate(Project project, VirtualFile file, MavenSettings settings) {
        Properties properties = new Properties();

        properties.setProperty("GROUP_ID", settings.groupId);
        properties.setProperty("ARTIFACT_ID", settings.artifactId);
        properties.setProperty("AUTHOR", settings.author == null ? "" : settings.author);
        properties.setProperty("VERSION", settings.version);
        properties.setProperty("REPO_ID", settings.repoId);
        properties.setProperty("REPO_URL", settings.repoUrl);
        properties.setProperty("API_NAME", settings.apiName);
        properties.setProperty("API_GROUP_ID", settings.apiGroupId);
        properties.setProperty("API_ARTIFACT_ID", settings.apiArtifactId);
        properties.setProperty("API_VERSION", settings.apiVersion);
        if (settings.hasAuthor())
            properties.setProperty("HAS_AUTHOR", "true");

        try {
            applyTemplate(project, file, BukkitFileTemplateGroupFactory.BUKKIT_POM_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyMainClassTemplate(Project project, VirtualFile file, String packageName, String className, boolean bukkit) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);
        if (bukkit)
            properties.setProperty("BUKKIT", "true");

        try {
            applyTemplate(project, file, BukkitFileTemplateGroupFactory.BUKKIT_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyPluginYmlTemplate(Project project, VirtualFile file, Type type, ProjectSettings settings, String groupId) {
        Properties properties = new Properties();

        properties.setProperty("NAME", settings.pluginName);
        properties.setProperty("VERSION", settings.pluginVersion);
        properties.setProperty("DESCRIPTION", settings.description);
        properties.setProperty("MAIN", groupId + "." + settings.mainClass);
        properties.setProperty("AUTHOR", settings.author);
        properties.setProperty("DEPEND", settings.depend.toString());
        properties.setProperty("SOFT_DEPEND", settings.softDepend.toString());

        if (settings.hasDescription())
            properties.setProperty("HAS_DESCRIPTION", "true");
        if (settings.hasAuthor())
            properties.setProperty("HAS_AUTHOR", "true");
        if (settings.hasDepend())
            properties.setProperty("HAS_DEPEND", "true");
        if (settings.hasSoftDepend())
            properties.setProperty("HAS_SOFT_DEPEND", "true");

        // These are bukkit and spigot only settings
        if (type != Type.BUNGEECORD) {
            properties.setProperty("WEBSITE", settings.website);
            properties.setProperty("DATABASE", Boolean.toString(settings.database));
            properties.setProperty("PREFIX", settings.prefix);
            properties.setProperty("LOAD", settings.load.name());
            properties.setProperty("AUTHOR_LIST", settings.authorList.toString());
            properties.setProperty("LOAD_BEFORE", settings.loadBefore.toString());

            if (settings.hasAuthorList())
                properties.setProperty("HAS_AUTHOR_LIST", "true");
            if (settings.hasWebsite())
                properties.setProperty("HAS_WEBSITE", "true");
            if (settings.hasDatabase())
                properties.setProperty("HAS_DATABASE", "true");
            if (settings.hasLoad())
                properties.setProperty("HAS_LOAD", "true");
            if (settings.hasPrefix())
                properties.setProperty("HAS_PREFIX", "true");
            if (settings.hasLoadBefore())
                properties.setProperty("HAS_LOAD_BEFORE", "true");
        }
        try {
            applyTemplate(project, file, BukkitFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void applyTemplate(Project project, VirtualFile file, String template, Properties properties) throws IOException {
        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate fileTemplate = manager.getTemplate(template);
        Properties allProperties = manager.getDefaultProperties();
        allProperties.putAll(properties);

        VfsUtil.saveText(file, fileTemplate.getText(allProperties));

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            new ReformatCodeProcessor(project, psiFile, null, false).run();
        }
    }
}
