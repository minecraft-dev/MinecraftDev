/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.util.Util;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CanaryProjectConfiguration extends ProjectConfiguration {

    public final List<String> dependencies = new ArrayList<>();
    public boolean enableEarly;
    public String canaryVersion;

    public boolean hasDependencies() {
        return listContainsAtLeastOne(dependencies);
    }

    public void setDependencies(String string) {
        this.dependencies.clear();
        Collections.addAll(this.dependencies, commaSplit(string));
    }

    @Override
    public void create(@NotNull Project project, @NotNull BuildSystem buildSystem, @NotNull ProgressIndicator indicator) {
        Util.runWriteTask(() -> {
            try {
                indicator.setText("Writing main class");
                // Create plugin main class
                VirtualFile file = buildSystem.getSourceDirectories().get(0);
                String[] files = this.mainClass.split("\\.");
                String className = files[files.length - 1];

                String packageName = this.mainClass.substring(0, this.mainClass.length() - className.length() - 1);
                file = getMainClassDirectory(files, file);

                VirtualFile mainClassFile = file.findOrCreateChildData(this, className + ".java");
                CanaryTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className);

                VirtualFile canaryInf = buildSystem.getResourceDirectories().get(0).findOrCreateChildData(this, "Canary.inf");
                CanaryTemplate.applyPluginDescriptionFileTemplate(project, canaryInf, this, buildSystem);

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
