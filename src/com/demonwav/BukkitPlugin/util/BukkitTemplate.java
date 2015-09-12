/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.BukkitPlugin.util;

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

        properties.setProperty("GROUP_ID", settings.getGroupId());
        properties.setProperty("ARTIFACT_ID", settings.getArtifactId());
        properties.setProperty("AUTHOR", settings.getAuthor() == null ? "" : settings.getAuthor());
        properties.setProperty("VERSION", settings.getVersion());
        properties.setProperty("REPO_ID", settings.getRepoId());
        properties.setProperty("REPO_URL", settings.getRepoUrl());
        properties.setProperty("API_NAME", settings.getApiName());
        properties.setProperty("API_GROUP_ID", settings.getApiGroupId());
        properties.setProperty("API_ARTIFACT_ID", settings.getApiArtifactId());
        properties.setProperty("API_VERSION", settings.getApiVersion());
        if (settings.hasAuthor())
            properties.setProperty("HAS_AUTHOR", "true");

        try {
            applyTemplate(project, file, BukkitFileTemplateGroupFactory.BUKKIT_POM_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyMainClassTemplate(Project project, VirtualFile file, String packageName, String className) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(project, file, BukkitFileTemplateGroupFactory.BUKKIT_MAIN_CLASS_TEMPLATE, properties);
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
