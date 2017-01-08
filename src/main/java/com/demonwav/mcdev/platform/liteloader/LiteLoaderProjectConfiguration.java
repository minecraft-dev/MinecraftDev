/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.util.Util;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LiteLoaderProjectConfiguration extends ProjectConfiguration {

    public String mcpVersion;
    public String mcVersion;

    public LiteLoaderProjectConfiguration() {
        type = PlatformType.LITELOADER;
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

                final VirtualFile mainClassFile = file.findOrCreateChildData(this, className + ".java");
                LiteLoaderTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className, pluginName, pluginVersion);

                final PsiJavaFile mainClassPsi = (PsiJavaFile) PsiManager.getInstance(project).findFile(mainClassFile);
                if (mainClassPsi == null) {
                    return;
                }

                EditorHelper.openInEditor(mainClassPsi);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
