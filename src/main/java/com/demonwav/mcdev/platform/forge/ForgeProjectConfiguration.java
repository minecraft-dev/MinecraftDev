/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.platform.forge.util.ForgeConstants;
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
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ForgeProjectConfiguration extends ProjectConfiguration {

    public List<String> dependencies = new ArrayList<>();
    public String updateUrl;

    public String mcpVersion;
    public String forgeVersion;

    public ForgeProjectConfiguration() {
        type = PlatformType.FORGE;
    }

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
                VirtualFile file = buildSystem.getSourceDirectories().get(0);
                final String[] files = this.mainClass.split("\\.");
                final String className = files[files.length - 1];
                final String packageName = this.mainClass.substring(0, this.mainClass.length() - className.length() - 1);
                file = getMainClassDirectory(files, file);

                VirtualFile mainClassFile = file.findOrCreateChildData(this, className + ".java");
                ForgeTemplate.applyMainClassTemplate(
                    project,
                    mainClassFile,
                    packageName,
                    buildSystem.getArtifactId(),
                    pluginName,
                    pluginVersion,
                    className
                );

                writeMcmodInfo(project, buildSystem);

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

    protected void writeMcmodInfo(Project project, BuildSystem buildSystem) {
        try {
            final VirtualFile file = buildSystem.getResourceDirectories().get(0);
            final VirtualFile mcmodInfoFile = file.findOrCreateChildData(this, ForgeConstants.MCMOD_INFO);

            String authorsText = "";
            if (hasAuthors()) {
                Iterator<String> iterator = authors.iterator();
                while (iterator.hasNext()) {
                    authorsText += '"' + iterator.next() + '"';
                    if (iterator.hasNext()) {
                        authorsText += ", ";
                    }
                }
            }
            if (authorsText.equals("")) {
                authorsText = null;
            }

            String dependenciesText = "";
            if (hasDependencies()) {
                final Iterator<String> iterator = dependencies.iterator();
                while (iterator.hasNext()) {
                    dependenciesText += '"' + iterator.next() + '"';
                    if (iterator.hasNext()) {
                        dependenciesText += ", ";
                    }
                }
                if (dependenciesText.equals("")) {
                    dependenciesText = null;
                }
            }

            ForgeTemplate.applyMcmodInfoTemplate(
                project,
                mcmodInfoFile,
                buildSystem.getArtifactId(),
                pluginName,
                description,
                website,
                updateUrl,
                authorsText,
                dependenciesText
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
