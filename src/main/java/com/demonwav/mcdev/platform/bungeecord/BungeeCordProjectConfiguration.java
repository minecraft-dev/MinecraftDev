/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.util.Util;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class BungeeCordProjectConfiguration extends ProjectConfiguration {

    public final List<String> dependencies = new ArrayList<>();
    public final List<String> softDependencies = new ArrayList<>();
    public String minecraftVersion;

    public BungeeCordProjectConfiguration() {
        type = PlatformType.BUNGEECORD;
    }

    public boolean hasDependencies() {
        return listContainsAtLeastOne(this.dependencies);
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

    @Override
    public void create(@NotNull Project project, @NotNull BuildSystem buildSystem, @NotNull ProgressIndicator indicator) {
        Util.runWriteTask(() -> {
            try {
                indicator.setText("Writing main class");
                // Create plugin main class
                VirtualFile file = buildSystem.getSourceDirectory();
                final String[] files = this.mainClass.split("\\.");
                final String className = files[files.length - 1];
                final String packageName = this.mainClass.substring(0, this.mainClass.length() - className.length() - 1);
                file = getMainClassDirectory(files, file);

                final VirtualFile mainClassFile = file.findOrCreateChildData(this, className + ".java");

                BungeeCordTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className);
                final VirtualFile pluginYml = buildSystem.getResourceDirectory().findOrCreateChildData(this, "plugin.yml");
                BungeeCordTemplate.applyPluginDescriptionFileTemplate(project, pluginYml, this, buildSystem);

                // Set the editor focus on the main class
                final PsiFile mainClassPsi = PsiManager.getInstance(project).findFile(mainClassFile);
                if (mainClassPsi != null) {
                    EditorHelper.openInEditor(mainClassPsi);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
