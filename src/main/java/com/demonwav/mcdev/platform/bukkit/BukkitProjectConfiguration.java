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
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BukkitProjectConfiguration extends ProjectConfiguration {

    @Nullable public LoadOrder loadOrder = null;
    public final List<String> loadBefore = new ArrayList<>();
    public final List<String> dependencies = new ArrayList<>();
    public final List<String> softDependencies = new ArrayList<>();
    public String prefix = null;

    public boolean hasPrefix() {
        return this.prefix != null && !this.prefix.trim().isEmpty();
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
    public void create(@NotNull Module module, @NotNull PlatformType type, @NotNull BuildSystem buildSystem) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // Create plugin main class
                VirtualFile file = buildSystem.getSourceDirectories().get(0);
                String[] files = this.mainClass.split("\\.");
                String className = files[files.length - 1];
                String packageName = this.mainClass.substring(0, this.mainClass.length() - className.length() - 1);
                for (int i = 0, len = files.length - 1; i < len; i++) {
                    String s = files[i];
                    file = file.createChildDirectory(this, s);
                }

                VirtualFile mainClassFile = file.findOrCreateChildData(this, className + ".java");

                BukkitTemplate.applyMainClassTemplate(module, mainClassFile, packageName, className);
                VirtualFile pluginYml = buildSystem.getResourceDirectories().get(0).findOrCreateChildData(this, "plugin.yml");
                BukkitTemplate.applyPluginDescriptionFileTemplate(module, pluginYml, this);

                // Set the editor focus on the main class
                PsiFile mainClassPsi = PsiManager.getInstance(module.getProject()).findFile(mainClassFile);
                if (mainClassPsi != null) {
                    EditorHelper.openInEditor(mainClassPsi);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        BukkitModuleType moduleType = null;
        switch (type) {
            case BUKKIT:
                moduleType = BukkitModuleType.getInstance();
                break;
            case SPIGOT:
                moduleType = SpigotModuleType.getInstance();
                break;
            case PAPER:
                moduleType = PaperModuleType.getInstance();
                break;
        }
        BukkitModule.setBuildSystem(module, moduleType, buildSystem);
    }
}
