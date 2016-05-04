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
import com.demonwav.mcdev.platform.ProjectConfiguration;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BukkitProjectConfiguration extends ProjectConfiguration {

    public Load load = Load.POSTWORLD;
    public final List<String> loadBefore = new ArrayList<>();
    public final List<String> dependencies = new ArrayList<>();
    public final List<String> softDependencies = new ArrayList<>();
    public String prefix = null;
    public String website = null;

    public boolean hasPrefix() {
        return this.prefix != null && !this.prefix.trim().isEmpty();
    }

    public boolean hasLoad() {
        return this.load == Load.STARTUP;
    }

    public boolean hasLoadBefore() {
        return listContainsAtLeastOne(this.loadBefore);
    }

    public void setLoadBefore(String string) {
        this.loadBefore.clear();
        Collections.addAll(this.loadBefore, commaSplit(string));
    }

    public boolean hasDependencies() {
        return listContainsAtLeastOne(dependencies);
    }

    public void setDependencies(String string) {
        this.dependencies.clear();
        Collections.addAll(this.dependencies, commaSplit(string));
    }

    public boolean hasSoftDependencies() {
        return listContainsAtLeastOne(this.softDependencies);
    }

    public void setSoftDependencies(String string) {
        this.softDependencies.clear();
        Collections.addAll(this.softDependencies, commaSplit(string));
    }

    public boolean hasWebsite() {
        return this.website != null && !this.website.trim().isEmpty();
    }

    @Override
    public void create(Project project, BuildSystem buildSystem) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // Create plugin main class
                VirtualFile file = buildSystem.getSourceDirectory();
                String[] files = this.mainClass.split("\\.");
                String className = files[files.length - 1];
                String packageName = this.mainClass.substring(0, this.mainClass.length() - className.length() - 1);
                for (int i = 0, len = files.length - 1; i < len; i++) {
                    String s = files[i];
                    file = file.createChildDirectory(this, s);
                }

                VirtualFile mainClassFile = file.findOrCreateChildData(this, className + ".java");

                BukkitTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className);
                VirtualFile pluginYml = buildSystem.getResourceDirectory().findOrCreateChildData(this, "plugin.yml");
                BukkitTemplate.applyPluginDescriptionFileTemplate(project, pluginYml, this);

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

    public enum Load {
        STARTUP,
        POSTWORLD
    }
}
