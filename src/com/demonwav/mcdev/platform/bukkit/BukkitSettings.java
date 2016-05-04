/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.MinecraftSettings;
import com.demonwav.mcdev.util.MinecraftTemplate;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.IOException;
import java.util.List;

public class BukkitSettings extends MinecraftSettings {

    public enum Load { STARTUP, POSTWORLD }

    public List<String> authorList = null;
    public String prefix = null;
    public Load load = Load.POSTWORLD;
    public List<String> loadBefore = null;
    public List<String> depend = null;
    public List<String> softDepend = null;

    public boolean hasAuthorList() {
        return testList(authorList);
    }

    public boolean hasWebsite() {
        return website != null && !website.trim().isEmpty();
    }

    public boolean hasLoad() {
        return load == Load.STARTUP;
    }

    public boolean hasPrefix() {
        return prefix != null && !prefix.trim().isEmpty();
    }

    public boolean hasDepend() {
        return testList(depend);
    }

    public boolean hasSoftDepend() {
        return testList(softDepend);
    }

    public boolean hasLoadBefore() {
        return testList(loadBefore);
    }

    @Override
    public void create(Project project, BuildSystem buildSystem) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // Create plugin main class
                VirtualFile file = buildSystem.getSourceDirectory();
                String[] files = buildSystem.getGroupId().split("\\.");
                for (String s : files) {
                    file = file.createChildDirectory(this, s);
                }

                VirtualFile mainClassFile = file.findOrCreateChildData(this, mainClass + ".java");

                MinecraftTemplate.applyMainBukkitClassTemplate(project, mainClassFile, buildSystem.getGroupId(), mainClass);
                VirtualFile pluginYml = buildSystem.getResourceDirectory().findOrCreateChildData(this, "plugin.yml");
                MinecraftTemplate.applyBukkitPluginYmlTemplate(project, pluginYml, this, buildSystem.getGroupId());

                // Set the editor focus on the main class
                PsiFile mainClassPsi = PsiManager.getInstance(project).findFile(mainClassFile);
                if (mainClassPsi != null) {
                    EditorHelper.openInEditor(mainClassPsi);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
