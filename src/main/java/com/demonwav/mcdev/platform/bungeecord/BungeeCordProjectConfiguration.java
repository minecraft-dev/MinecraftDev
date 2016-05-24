package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BungeeCordProjectConfiguration extends ProjectConfiguration {

    public final List<String> dependencies = new ArrayList<>();
    public final List<String> softDependencies = new ArrayList<>();
    public String minecraftVersion;

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
    public void create(@NotNull Module module, @NotNull PlatformType type, @NotNull BuildSystem buildSystem, @NotNull ProgressIndicator indicator) {
        ApplicationManager.getApplication().invokeAndWait(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                indicator.setText("Writing main class");
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

                BungeeCordTemplate.applyMainClassTemplate(module, mainClassFile, packageName, className);
                VirtualFile pluginYml = buildSystem.getResourceDirectories().get(0).findOrCreateChildData(this, "plugin.yml");
                BungeeCordTemplate.applyPluginDescriptionFileTemplate(module, pluginYml, this);

                // Set the editor focus on the main class
                PsiFile mainClassPsi = PsiManager.getInstance(module.getProject()).findFile(mainClassFile);
                if (mainClassPsi != null) {
                    EditorHelper.openInEditor(mainClassPsi);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }), ModalityState.any());
    }
}
